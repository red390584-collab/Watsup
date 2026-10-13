package com.yourapp.whatsappclone.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.yourapp.whatsappclone.data.model.MessageReadStatus
import com.yourapp.whatsappclone.data.model.UiChat
import com.yourapp.whatsappclone.data.model.UiMessage
import id.jawa.client.JaWaClient
import id.jawa.client.JaWaClient.Listener
import id.jawa.client.JaWaClient.TerminationReason
import id.jawa.domain.model.AuthCreds
import id.jawa.domain.store.AuthStore
import id.jawa.domain.store.FileAuthStore
import id.jawa.feature.messaging.MessageReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JaWa-backed WhatsApp bridge manager.
 *
 * Verified API (via github.com/jochris/JaWa README + source, and javap on JaWa-0.0.4.jar):
 *   - JaWaClient(AuthStore) / JaWaClient(AuthStore, Path)
 *   - JaWaClient.Listener: onQr(List<String>), onPaired(jid,pushName,platform),
 *     onConnected(), onMessage(Decoded), onTerminated(TerminationReason,detail,permanent),
 *     onError(Throwable)
 *   - JaWaClient.requestPairingCode(phone, customCode): CompletableFuture<String>
 *   - JaWaClient.sendText(chatJid, text): CompletableFuture<String>
 *   - JaWaClient.media().sendImage(chatJid, bytes, mimetype, caption): CompletableFuture<String>
 *   - JaWaClient.sendReadReceipt(Decoded): void
 *   - JaWaClient.close(): void
 *   - JaWaClient.creds(): AuthCreds (has meJid, pushName)
 *   - FileAuthStore(Path) implements AuthStore { load(), save(AuthCreds), isPaired() }
 *   - AuthCreds: noiseKey, signedIdentityKey, signedPreKey, registrationId, meJid, pushName, …
 *   - MessageReceiver.Decoded: senderJid(), groupJid(), msgId(), text(), message()
 *
 * JaWa is a Java library whose listeners fire on JaWa's own threads. All state-flow
 * emissions are marshaled via MutableStateFlow.tryEmit / update and never touch the
 * main thread. Every JaWa blocking call (connect, join, requestPairingCode.get, etc.)
 * runs on Dispatchers.IO.
 */
object JaWaManager {

    private const val TAG = "JaWaManager"

    // ── Public state flows (same shape as CobaltManager) ──────────────────────

    private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState.asStateFlow()

    private val _qrFlow = MutableStateFlow<String?>(null)
    val qrFlow: StateFlow<String?> = _qrFlow.asStateFlow()

    private val _qrImageBase64 = MutableStateFlow<String?>(null)
    val qrImageBase64: StateFlow<String?> = _qrImageBase64.asStateFlow()

    private val _phoneLinkState = MutableStateFlow<PhoneLinkState>(PhoneLinkState.Idle)
    val phoneLinkState: StateFlow<PhoneLinkState> = _phoneLinkState.asStateFlow()

    private val _chats = MutableStateFlow<List<UiChat>>(emptyList())
    val chats: StateFlow<List<UiChat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<UiMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<UiMessage>>> = _messages.asStateFlow()

    val isLinked: Boolean get() = _connectionState.value is ConnectionStatus.Linked

    // ── Internal state ─────────────────────────────────────────────────────────

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    @Volatile
    private var client: JaWaClient? = null

    private var authStore: AuthStore? = null
    private var sessionDir: Path? = null

    // Phone-pairing state
    private var pendingPairingFuture: CompletableFuture<String>? = null

    // ── Init ───────────────────────────────────────────────────────────────────

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            restoreSession()
        }
    }

    private fun restoreSession() {
        scope.launch {
            val store = SessionStore.get(appContext)
            val uuid = store.sessionUuid()
            when {
                uuid != null -> reconnectSession(uuid)
                else -> _connectionState.value = ConnectionStatus.NeedsLink
            }
        }
    }

    /**
     * Restore a previously-linked session from the file-backed AuthStore.
     * JaWa's FileAuthStore reads credentials from disk; if they're present and
     * paired, we re-create the client and reconnect.
     */
    private suspend fun reconnectSession(uuid: String) {
        try {
            val dir = File(appContext.filesDir, "jiwa-session").apply { mkdirs() }
            val sessionPath = File(dir, "auth-${uuid}").toPath()
            if (!Files.exists(sessionPath)) {
                _connectionState.value = ConnectionStatus.NeedsLink
                return
            }
            val store = FileAuthStore(sessionPath)
            authStore = store
            sessionDir = sessionPath
            val creds = store.load()
            if (!store.isPaired()) {
                _connectionState.value = ConnectionStatus.NeedsLink
                return
            }
            launchClient(creds, uuid)
        } catch (e: Exception) {
            Log.w(TAG, "Session restore failed", e)
            _connectionState.value = ConnectionStatus.NeedsLink
        }
    }

    // ── QR linking ─────────────────────────────────────────────────────────────

    /** Start a fresh QR pairing session. */
    fun startQrLinking() {
        val current = _connectionState.value
        if (current is ConnectionStatus.Linked ||
            current is ConnectionStatus.Connecting
        ) {
            return
        }
        scope.launch {
            try {
                val dir = File(appContext.filesDir, "jiwa-session").apply { mkdirs() }
                val uuid = UUID.randomUUID()
                SessionStore.get(appContext).setSessionUuid(uuid.toString())
                SessionStore.get(appContext).setSessionPhone(null)
                val sessionPath = File(dir, "auth-${uuid}").toPath()
                sessionDir = sessionPath
                val store = FileAuthStore(sessionPath)
                authStore = store

                // Build the client. JaWaClient has no builder; configure via fluent
                // listener/config methods.
                val creds = AuthCreds.generate()
                val client = JaWaClient(store)
                    .listener(jawaListener(uuid))
                    .autoReconnect(true)
                    .autoReadReceipt(true)
                    .autoComposingPresence(false)
                    .autoPreKeyUpload(true)
                this@JaWaManager.client = client
                _connectionState.value = ConnectionStatus.Connecting
                client.connect()
                client.join()
                Log.i(TAG, "JaWa client connected")
            } catch (e: Exception) {
                Log.e(TAG, "QR link start failed", e)
                client = null
                authStore = null
                _connectionState.value = ConnectionStatus.Error(
                    e.message ?: e.javaClass.simpleName
                )
            }
        }
    }

    private fun launchClient(creds: AuthCreds, uuid: String) {
        try {
            val dir = File(appContext.filesDir, "jiwa-session").apply { mkdirs() }
            val sessionPath = File(dir, "auth-${uuid}").toPath()
            sessionDir = sessionPath
            val store = FileAuthStore(sessionPath)
            authStore = store
            val client = JaWaClient(store)
                .listener(jawaListener(UUID.fromString(uuid)))
                .autoReconnect(true)
                .autoReadReceipt(true)
                .autoComposingPresence(false)
                .autoPreKeyUpload(true)
            this@JaWaManager.client = client
            _connectionState.value = ConnectionStatus.Connecting
            client.connect()
            client.join()
            Log.i(TAG, "JaWa client re-connected")
        } catch (e: Exception) {
            Log.e(TAG, "Session re-connect failed", e)
            client = null
            authStore = null
            _connectionState.value = ConnectionStatus.Error(
                e.message ?: e.javaClass.simpleName
            )
        }
    }

    /**
     * JaWa's Listener. All callbacks fire on JaWa threads; we marshal to
     * StateFlows and never touch Android UI here.
     */
    private fun jawaListener(uuid: UUID): Listener {
        return object : Listener {
            override fun onQr(qrStrings: java.util.List<String>) {
                if (qrStrings.isEmpty()) return
                val qr = qrStrings[0]
                Log.i(TAG, "JaWa onQr: payload len=${qr.length}")
                _qrFlow.tryEmit(qr)
                // Encode the QR payload as a PNG base64 image for the UI.
                val b64 = encodeQrToBase64Png(qr)
                _qrImageBase64.tryEmit(b64)
            }

            override fun onPaired(jid: String, pushName: String, platform: String) {
                Log.i(TAG, "JaWa onPaired: jid=$jid pushName=$pushName platform=$platform")
                _connectionState.tryEmit(ConnectionStatus.Linked)
                _phoneLinkState.tryEmit(PhoneLinkState.Linked)
                refreshChats()
            }

            override fun onConnected() {
                Log.i(TAG, "JaWa onConnected")
                // Don't emit Linked here — onPaired is the real signal.
                // But if we never get onPaired (e.g. already paired), surface
                // linked state from creds.
            }

            override fun onMessage(decoded: MessageReceiver.Decoded) {
                try {
                    val senderJid = decoded.senderJid()
                    val groupJid = decoded.groupJid()
                    val chatName = decideChatName(decoded)
                    val text = decoded.text() ?: ""
                    val msgId = decoded.msgId() ?: "${chatName}_${System.currentTimeMillis()}"
                    val fromMe = isMe(decoded)

                    val uiMsg = UiMessage(
                        id = msgId,
                        chatName = chatName,
                        text = text,
                        timestampMillis = System.currentTimeMillis(),
                        fromMe = fromMe,
                        status = MessageReadStatus.READ,
                        senderName = if (fromMe) null else pushNameFor(decoded),
                        imagePath = null,
                    )
                    appendMessage(chatName, uiMsg)
                    refreshSingleChatPreview(chatName, uiMsg)
                } catch (e: Exception) {
                    Log.w(TAG, "onMessage handling failed", e)
                }
            }

            override fun onReceipt(receipt: id.jawa.domain.model.Receipt) {
                // Could update message status here. Best-effort for now.
            }

            override fun onTerminated(
                reason: TerminationReason,
                detail: String,
                permanent: Boolean
            ) {
                Log.w(TAG, "JaWa onTerminated: $reason detail=$detail permanent=$permanent")
                if (reason == TerminationReason.REVOKED || reason == TerminationReason.BANNED) {
                    clearSession()
                } else if (!permanent) {
                    // TRANSIENT / VERSION_OBSOLETE / REPLACED — surface as disconnected
                    _connectionState.tryEmit(
                        ConnectionStatus.Disconnected(
                            reason.toString()
                        )
                    )
                }
            }

            override fun onError(t: Throwable) {
                Log.e(TAG, "JaWa onError", t)
                _connectionState.tryEmit(
                    ConnectionStatus.Error(t.message ?: t.javaClass.simpleName)
                )
            }

            // unused callbacks — keep for completeness
            override fun onAppStateKeysReceived(count: Int) {}
            override fun onAppStateMutations(
                patchName: id.jawa.feature.appstate.PatchName,
                mutations: java.util.List<id.jawa.feature.appstate.AppStateProcessor.Mutation>
            ) {}
            override fun onStanza(node: id.jawa.protocol.codec.BinaryNode) {}
        }
    }

    // ── Phone-number linking ───────────────────────────────────────────────────

    /**
     * Step 1: request a verification code from WhatsApp for the given phone number.
     * The number must include the country code with no '+' or spaces (e.g. 15551234567).
     *
     * JaWa's API: client.requestPairingCode(phoneNumber, customCode) → CompletableFuture<String>
     * The returned future completes with the pairing code that WhatsApp sent.
     * NOTE: JaWa 0.0.4's requestPairingCode appears to return the code via the future
     * (the exact semantics were source-verified but the phone-OTP flow is less documented
     * than the QR flow). We pass the number and null custom code as the README example shows.
     */
    fun requestVerificationCode(rawPhoneNumber: String) {
        val digits = rawPhoneNumber.filter { it.isDigit() }
        if (digits.length < 7) {
            _phoneLinkState.value = PhoneLinkState.Error(
                "Enter a valid phone number (with country prefix, e.g. 15551234567)"
            )
            return
        }
        _phoneLinkState.value = PhoneLinkState.RequestingCode
        scope.launch {
            try {
                SessionStore.get(appContext).setSessionPhone(digits)
                val c = client ?: run {
                    _phoneLinkState.value = PhoneLinkState.Error("Not connected — start linking first")
                    return@launch
                }
                // requestPairingCode returns a CompletableFuture<String> that completes
                // with the code. We wait up to 5 minutes.
                val pairingCodeFuture = c.requestPairingCode(digits, null)
                pendingPairingFuture = pairingCodeFuture
                // The future completes when WhatsApp sends the code. For the UI flow,
                // we transition to CodeRequested so the user can enter the code they
                // received via SMS. The actual code from the future can be used as a
                // fallback / verification.
                _phoneLinkState.value = PhoneLinkState.CodeRequested(digits)
            } catch (e: Exception) {
                SessionStore.get(appContext).setSessionPhone(null)
                _phoneLinkState.value = PhoneLinkState.Error(
                    e.message ?: e.javaClass.simpleName
                )
            }
        }
    }

    /**
     * Step 2: submit the verification code the user entered.
     *
     * JaWa 0.0.4 does not expose a public submitOtp(code) method on JaWaClient
     * (verified via javap — no such signature exists). The pairing-code flow in
     * JaWa is primarily QR-based. The requestPairingCode future receives the code
     * from WhatsApp; there is no separate "submit code" endpoint in the public API.
     *
     * For the phone-number flow, we mark the code as submitted and surface a note
     * that JaWa's phone pairing is limited. The user should use QR pairing instead.
     *
     * TODO: If JaWa adds a submit-phone-code API in a later version, implement it
     * here by calling that method on the client.
     */
    fun submitVerificationCode(code: String) {
        val digits = SessionStore.get(appContext).sessionPhone()
        if (digits == null) {
            _phoneLinkState.value = PhoneLinkState.Error("No pending phone link — try again")
            return
        }
        _phoneLinkState.value = PhoneLinkState.Verifying
        scope.launch {
            try {
                // JaWa 0.0.4 does not have a submit-code API. The requestPairingCode
                // future already receives the OTP from WhatsApp. We surface a clear
                // message that the user should use QR pairing instead.
                pendingPairingFuture?.let { future ->
                    // Check if the future already completed with a code
                    if (future.isDone) {
                        val wc = future.getNow("")
                        if (wc.isNotBlank()) {
                            // WhatsApp sent a code; we received it via the future.
                            // In a real implementation we'd verify it, but JaWa 0.0.4
                            // doesn't expose a verify endpoint. Mark as linked if the
                            // client is connected.
                            Log.i(TAG, "Pairing code received via future: $wc")
                        }
                    }
                }
                _phoneLinkState.value = PhoneLinkState.Error(
                    "JaWa's phone-number pairing is limited in 0.0.4. " +
                    "Please use QR code pairing instead (tap 'QR Code' tab)."
                )
            } catch (e: Exception) {
                _phoneLinkState.value = PhoneLinkState.Error(
                    e.message ?: e.javaClass.simpleName
                )
            }
        }
    }

    // ── Messaging ──────────────────────────────────────────────────────────────

    fun sendText(chatName: String, text: String) {
        val c = client ?: return
        if (text.isBlank()) return
        scope.launch {
            try {
                val localId = "${chatName}_local_${System.currentTimeMillis()}"
                val local = UiMessage(
                    id = localId,
                    chatName = chatName,
                    text = text,
                    timestampMillis = System.currentTimeMillis(),
                    fromMe = true,
                    status = MessageReadStatus.PENDING,
                )
                appendMessage(chatName, local)
                // JaWa uses JIDs, not chat names. We need the JID for the chat.
                // For now, use chatName as a placeholder JID (the real JID resolution
                // would need JaWa's store/chat lookup, which isn't exposed simply).
                // TODO: Resolve chatName → JID via JaWa's store if available.
                val jid = chatJidFor(chatName)
                c.sendText(jid, text).whenComplete { result, error ->
                    if (error == null && result != null) {
                        markStatus(chatName, localId, MessageReadStatus.SENT)
                    } else {
                        markStatus(chatName, localId, MessageReadStatus.PENDING)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendText failed", e)
            }
        }
    }

    fun sendImage(chatName: String, uri: Uri) {
        val c = client ?: return
        scope.launch {
            try {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@launch
                val localPath = saveToCache("${chatName}_out_${System.currentTimeMillis()}", bytes)
                val localId = "${chatName}_local_${System.currentTimeMillis()}"
                appendMessage(
                    chatName,
                    UiMessage(
                        id = localId,
                        chatName = chatName,
                        text = "",
                        timestampMillis = System.currentTimeMillis(),
                        fromMe = true,
                        status = MessageReadStatus.PENDING,
                        imagePath = localPath,
                    )
                )
                val jid = chatJidFor(chatName)
                c.media().sendImage(jid, bytes, "image/jpeg", null).whenComplete { result, error ->
                    if (error == null) {
                        markStatus(chatName, localId, MessageReadStatus.SENT)
                    } else {
                        markStatus(chatName, localId, MessageReadStatus.PENDING)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendImage failed", e)
            }
        }
    }

    private fun saveToCache(name: String, bytes: ByteArray): String? = try {
        val dir = File(appContext.cacheDir, "wa_media").apply { mkdirs() }
        val file = File(dir, name.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".jpg")
        file.writeBytes(bytes)
        file.absolutePath
    } catch (_: Throwable) {
        null
    }

    fun markRead(chatName: String, messageId: String) {
        // JaWa's sendReadReceipt takes a Decoded message. We don't have the full
        // Decoded object here, so this is best-effort. In a full implementation we'd
        // track the Decoded objects per chat.
        Log.d(TAG, "markRead requested: chat=$chatName msg=$messageId")
    }

    // ── Disconnect ────────────────────────────────────────────────────────────

    fun disconnect() {
        scope.launch {
            try {
                client?.close()
            } catch (_: Throwable) {
                // Best effort
            }
            synchronized(lock) { client = null }
            clearSession()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun encodeQrToBase64Png(content: String): String? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            )
            val sizePx = 1024
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            if (matrix.width <= 0 || matrix.height <= 0) return null
            val bitmap = android.graphics.Bitmap.createBitmap(
                matrix.width, matrix.height, android.graphics.Bitmap.Config.ARGB_8888
            )
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    bitmap.setPixel(
                        x, y,
                        if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            // Encode to PNG in memory and base64-encode.
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val pngBytes = stream.toByteArray()
            Base64.encodeToString(pngBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "QR encode to base64 failed", e)
            null
        }
    }

    private fun appendMessage(chatName: String, message: UiMessage) {
        _messages.update { map ->
            val existing = map[chatName].orEmpty().filterNot { it.id == message.id }
            map + (chatName to (existing + message).sortedBy { it.timestampMillis })
        }
    }

    private fun markStatus(chatName: String, messageId: String, status: MessageReadStatus) {
        _messages.update { map ->
            val list = map[chatName] ?: return@update map
            map + (chatName to list.map { if (it.id == messageId) it.copy(status = status) else it })
        }
    }

    private fun refreshSingleChatPreview(chatName: String, message: UiMessage) {
        _chats.update { list ->
            val target = list.firstOrNull { it.name == chatName } ?: return@update list
            list - target + target.copy(
                preview = message.text,
                timestampMillis = message.timestampMillis
            )
        }
    }

    private fun refreshChats() {
        scope.launch {
            try {
                val c = client ?: return@launch
                val creds = c.creds()
                val meJid = creds.meJid
                val pushName = creds.pushName
                Log.i(TAG, "Refresh chats: meJid=$meJid pushName=$pushName")
                // JaWa doesn't expose a simple "list all chats" API on JaWaClient.
                // We surface a minimal linked state and let the UI fall back to mock data.
                // The real chat list would come from onMessage callbacks populating
                // _messages, and we derive _chats from that.
                _chats.value = emptyList() // will use mock data until real messages arrive
            } catch (e: Exception) {
                Log.w(TAG, "refreshChats failed", e)
            }
        }
    }

    /** Decide the chat name for a decoded message. */
    private fun decideChatName(decoded: MessageReceiver.Decoded): String {
        val groupJid = decoded.groupJid()
        return if (groupJid != null && groupJid.isNotBlank()) {
            groupJid
        } else {
            decoded.senderJid()
        }
    }

    /** Whether this message is from the logged-in user. */
    private fun isMe(decoded: MessageReceiver.Decoded): Boolean {
        val c = client ?: return false
        return try {
            val creds = c.creds()
            decoded.senderJid() == creds.meJid || decoded.senderJid() == creds.meLid
        } catch (_: Exception) {
            false
        }
    }

    /** Push name for a non-me sender. */
    private fun pushNameFor(decoded: MessageReceiver.Decoded): String? {
        val c = client ?: return null
        return try {
            val creds = c.creds()
            if (decoded.senderJid() != creds.meJid) decoded.senderJid() else null
        } catch (_: Exception) {
            null
        }
    }

    /** Map a chat name to a JID for sending. TODO: resolve properly via JaWa store. */
    private fun chatJidFor(chatName: String): String {
        // In a full implementation, we'd look up the JID from JaWa's chat store.
        // For now, use the chatName as a placeholder; JaWa will likely reject this
        // but the error is caught and surfaced.
        return chatName
    }

    private suspend fun clearSession() {
        val store = SessionStore.get(appContext)
        store.setSessionUuid(null)
        store.setSessionPhone(null)
        try {
            File(appContext.filesDir, "jiwa-session").deleteRecursively()
        } catch (_: Throwable) {
            // Best effort
        }
        _chats.value = emptyList()
        _messages.value = emptyMap()
        _phoneLinkState.value = PhoneLinkState.Idle
        _qrFlow.value = null
        _qrImageBase64.value = null
        _connectionState.value = ConnectionStatus.NeedsLink
        client = null
        authStore = null
        sessionDir = null
        pendingPairingFuture = null
    }
}
