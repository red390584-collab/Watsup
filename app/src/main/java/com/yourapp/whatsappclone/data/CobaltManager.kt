package com.yourapp.whatsappclone.data

import android.content.Context
import android.net.Uri
import com.yourapp.whatsappclone.data.model.MessageReadStatus
import com.yourapp.whatsappclone.data.model.UiChat
import com.yourapp.whatsappclone.data.model.UiMessage
import it.auties.whatsapp.api.DisconnectReason
import it.auties.whatsapp.api.MobileRegistrationBuilder
import it.auties.whatsapp.api.QrHandler
import it.auties.whatsapp.api.Whatsapp
import it.auties.whatsapp.controller.ControllerSerializer
import it.auties.whatsapp.listener.ListenerConsumer
import it.auties.whatsapp.model.chat.Chat
import it.auties.whatsapp.model.info.ChatMessageInfo
import it.auties.whatsapp.model.message.model.MessageStatus
import it.auties.whatsapp.model.message.standard.ImageMessageSimpleBuilder
import it.auties.whatsapp.model.sync.HistorySyncMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.time.ZonedDateTime

/** High-level connection status surfaced to the UI. */
sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object NeedsLink : ConnectionStatus()
    object Connecting : ConnectionStatus()
    data class QrReady(val payload: String) : ConnectionStatus()
    object Linked : ConnectionStatus()
    data class Disconnected(val reason: DisconnectReason) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

/** State machine for the phone-number (OTP) linking flow. */
sealed class PhoneLinkState {
    object Idle : PhoneLinkState()
    object RequestingCode : PhoneLinkState()
    data class CodeRequested(val phoneNumber: String) : PhoneLinkState()
    object Verifying : PhoneLinkState()
    object Linked : PhoneLinkState()
    data class Error(val message: String) : PhoneLinkState()
}

/**
 * Single point of contact with the Cobalt WhatsApp client. Every Cobalt call
 * is blocking by contract, so all of them run on [Dispatchers.IO] inside
 * [scope] — nothing here ever touches the main thread.
 */
object CobaltManager {

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    @Volatile
    private var api: Whatsapp? = null

    private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState.asStateFlow()

    private val _phoneLinkState = MutableStateFlow<PhoneLinkState>(PhoneLinkState.Idle)
    val phoneLinkState: StateFlow<PhoneLinkState> = _phoneLinkState.asStateFlow()

    private val _chats = MutableStateFlow<List<UiChat>>(emptyList())
    val chats: StateFlow<List<UiChat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<UiMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<UiMessage>>> = _messages.asStateFlow()

    private var codeBridge: CompletableFuture<String>? = null

    val isLinked: Boolean get() = api != null && _connectionState.value is ConnectionStatus.Linked

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            restoreSession()
        }
    }

    // ── Session restore ────────────────────────────────────────────────────────

    private fun restoreSession() {
        scope.launch {
            val store = SessionStore.get(appContext)
            val uuid = store.sessionUuid()
            val phone = store.sessionPhone()
            when {
                uuid != null -> connectWeb(UUID.fromString(uuid))
                phone != null -> connectMobileRegistered(phone)
                else -> _connectionState.value = ConnectionStatus.NeedsLink
            }
        }
    }

    // ── QR linking ─────────────────────────────────────────────────────────────

    /** Starts (or restarts) the QR pairing flow with a brand new session. */
    fun startQrLinking() {
        val current = _connectionState.value
        if (current is ConnectionStatus.Linked ||
            current is ConnectionStatus.Connecting ||
            current is ConnectionStatus.QrReady
        ) {
            return
        }
        scope.launch {
            val store = SessionStore.get(appContext)
            val uuid = UUID.randomUUID()
            store.setSessionUuid(uuid.toString())
            store.setSessionPhone(null)
            connectWeb(uuid)
        }
    }

    private fun connectWeb(uuid: UUID) {
        scope.launch {
            synchronized(lock) {
                if (api != null) return@launch
            }
            try {
                _connectionState.value = ConnectionStatus.Connecting
                val options = Whatsapp.webBuilder()
                    .serializer(sessionSerializer())
                    .newConnection(uuid)
                    .name("WhatsApp Clone")
                    .automaticMessageReceipts(true)
                val existing = options.registered()
                val whatsapp = if (existing.isPresent) {
                    existing.get()
                } else {
                    options.unregistered(QrHandler.toPlainString { payload ->
                        _connectionState.value = ConnectionStatus.QrReady(payload)
                    })
                }
                attachListeners(whatsapp)
                api = whatsapp
                whatsapp.connect().join()
            } catch (error: Throwable) {
                api = null
                _connectionState.value = ConnectionStatus.Error(
                    error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    // ── Phone-number linking ───────────────────────────────────────────────────

    /** Step 1 of the OTP flow: ask WhatsApp to send a verification code. */
    fun requestVerificationCode(rawPhoneNumber: String) {
        val digits = rawPhoneNumber.filter { it.isDigit() }
        if (digits.length < 7) {
            _phoneLinkState.value = PhoneLinkState.Error("Enter a valid number with country prefix")
            return
        }
        _phoneLinkState.value = PhoneLinkState.RequestingCode
        scope.launch {
            try {
                SessionStore.get(appContext).setSessionPhone(digits)
                val registration = Whatsapp.mobileBuilder()
                    .serializer(sessionSerializer())
                    .newConnection(digits.toLong())
                    .unregistered()
                val result = registration
                    .requestVerificationCode(digits.toLong())
                    .join(5, TimeUnit.MINUTES)
                val bridge = CompletableFuture<String>()
                codeBridge = bridge
                _phoneLinkState.value = PhoneLinkState.CodeRequested(digits)
                result.unverified()
                    .verificationCodeSupplier(Supplier<String> { bridge.get(10, TimeUnit.MINUTES) })
                    .also { pendingUnverified = it }
            } catch (error: Throwable) {
                SessionStore.get(appContext).setSessionPhone(null)
                _phoneLinkState.value = PhoneLinkState.Error(
                    error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    @Volatile
    private var pendingUnverified: MobileRegistrationBuilder.Unverified? = null

    /** Step 2 of the OTP flow: submit the code the user typed in. */
    fun submitVerificationCode(code: String) {
        val bridge = codeBridge ?: return
        val unverified = pendingUnverified ?: return
        _phoneLinkState.value = PhoneLinkState.Verifying
        bridge.complete(code)
        scope.launch {
            try {
                val result = unverified.verify().join(5, TimeUnit.MINUTES)
                val whatsapp = result.whatsapp()
                attachListeners(whatsapp)
                api = whatsapp
                _phoneLinkState.value = PhoneLinkState.Linked
                _connectionState.value = ConnectionStatus.Linked
                whatsapp.connect().join()
                refreshChats(whatsapp)
            } catch (error: Throwable) {
                _phoneLinkState.value = PhoneLinkState.Error(
                    error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    private fun connectMobileRegistered(phone: String) {
        scope.launch {
            try {
                _connectionState.value = ConnectionStatus.Connecting
                val options = Whatsapp.mobileBuilder()
                    .serializer(sessionSerializer())
                    .newConnection(phone.toLong())
                val registered = options.registered()
                if (!registered.isPresent) {
                    _connectionState.value = ConnectionStatus.NeedsLink
                    return@launch
                }
                val whatsapp = registered.get()
                attachListeners(whatsapp)
                api = whatsapp
                whatsapp.connect().join()
            } catch (error: Throwable) {
                api = null
                _connectionState.value = ConnectionStatus.NeedsLink
            }
        }
    }

    // ── Listeners ──────────────────────────────────────────────────────────────

    private fun attachListeners(whatsapp: Whatsapp) {
        whatsapp.addLoggedInListener(ListenerConsumer.Empty {
            _connectionState.value = ConnectionStatus.Linked
            _phoneLinkState.value = PhoneLinkState.Linked
            scope.launch { refreshChats(whatsapp) }
        })
        whatsapp.addDisconnectedListener { reason ->
            if (reason == DisconnectReason.LOGGED_OUT) {
                scope.launch { clearSession() }
            } else if (reason != DisconnectReason.RECONNECTING && reason != DisconnectReason.RESTORE) {
                _connectionState.value = ConnectionStatus.Disconnected(reason)
            }
        }
        whatsapp.addNewChatMessageListener { info ->
            onIncomingMessage(info)
        }
    }

    // ── Chats & messages ───────────────────────────────────────────────────────

    private fun refreshChats(whatsapp: Whatsapp) {
        scope.launch {
            try {
                val store = whatsapp.store()
                val sorted = store.chats()
                    .sortedByDescending { chat ->
                        newestTimestamp(chat.newestMessage().orElse(null), chat.timestampSeconds())
                    }
                val uiChats = sorted.map { chat -> toUiChat(chat) }
                _chats.value = uiChats
                val seeded = sorted.associate { chat ->
                    val name = chat.name()
                    val history = chat.messages()
                        .mapNotNull { entry: HistorySyncMessage -> entry.messageInfo() }
                        .sortedBy { info -> messageTimestampMillis(info) }
                        .map { info -> toUiMessage(info, name) }
                    name to history.takeLast(200)
                }
                _messages.update { current -> seeded + current }
            } catch (_: Throwable) {
                // Keep whatever we had; the UI falls back to mock data.
            }
        }
    }

    private fun newestTimestamp(info: ChatMessageInfo?, fallback: Long): Long =
        if (info != null) messageTimestampMillis(info) else fallback

    private fun toUiChat(chat: Chat): UiChat {
        val newest = chat.newestMessage().orElse(null)
        val preview = newest?.let { previewText(it) } ?: ""
        return UiChat(
            name = chat.name(),
            subtitle = chat.jid().user() ?: "",
            preview = preview,
            timestampMillis = newestTimestamp(newest, chat.timestampSeconds()),
            unreadCount = chat.unreadMessagesCount(),
            isGroup = chat.isGroup(),
            isPinned = chat.isPinned(),
        )
    }

    private fun previewText(info: ChatMessageInfo): String {
        val container = info.message()
        val text = container.textMessage().map { it.text() }.orElse(null)
            ?: container.imageMessage().map { image -> image.caption().orElse("📷 Photo") }.orElse(null)
            ?: "Attachment"
        return if (info.fromMe()) "You: $text" else text
    }

    private fun onIncomingMessage(info: ChatMessageInfo) {
        scope.launch {
            try {
                val name = info.chatName()
                val message = toUiMessage(info, name)
                appendMessage(name, message)
                refreshSingleChatPreview(name)
            } catch (_: Throwable) {
                // Never crash the socket thread.
            }
        }
    }

    private fun refreshSingleChatPreview(name: String) {
        _chats.update { list ->
            val target = list.firstOrNull { it.name == name } ?: return@update list
            val message = _messages.value[name]?.lastOrNull() ?: return@update list
            list - target + target.copy(preview = message.text, timestampMillis = message.timestampMillis)
        }
    }

    private fun toUiMessage(info: ChatMessageInfo, chatName: String): UiMessage {
        val container = info.message()
        val textMessage = container.textMessage().orElse(null)
        val imageMessage = container.imageMessage().orElse(null)
        val text = when {
            textMessage != null -> textMessage.text()
            imageMessage != null -> imageMessage.caption().orElse(null) ?: "📷 Photo"
            else -> "Attachment"
        }
        val imagePath = imageMessage?.thumbnail()?.orElse(null)?.let { thumb ->
            saveToCache("${chatName}_${info.id()}", thumb)
        }
        return UiMessage(
            id = "${chatName}_${info.id()}",
            chatName = chatName,
            text = text,
            timestampMillis = messageTimestampMillis(info),
            fromMe = info.fromMe(),
            status = mapStatus(info.status()),
            senderName = info.senderName(),
            imagePath = imagePath,
        )
    }

    private fun messageTimestampMillis(info: ChatMessageInfo): Long =
        info.timestamp()
            .map { zdt: ZonedDateTime -> zdt.toInstant().toEpochMilli() }
            .orElse(System.currentTimeMillis())

    private fun mapStatus(status: MessageStatus): MessageReadStatus = when (status) {
        MessageStatus.PENDING, MessageStatus.ERROR -> MessageReadStatus.PENDING
        MessageStatus.SERVER_ACK -> MessageReadStatus.SENT
        MessageStatus.DELIVERED -> MessageReadStatus.DELIVERED
        MessageStatus.READ, MessageStatus.PLAYED -> MessageReadStatus.READ
    }

    // ── Sending ────────────────────────────────────────────────────────────────

    fun sendText(chatName: String, text: String) {
        val whatsapp = api ?: return
        if (text.isBlank()) return
        scope.launch {
            val chat = whatsapp.store().findChatByName(chatName).orElse(null) ?: return@launch
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
            try {
                whatsapp.sendMessage(chat, text).whenComplete { _, error ->
                    if (error == null) {
                        markStatus(chatName, localId, MessageReadStatus.SENT)
                    } else {
                        markStatus(chatName, localId, MessageReadStatus.PENDING)
                    }
                }
            } catch (_: Throwable) {
                markStatus(chatName, localId, MessageReadStatus.PENDING)
            }
        }
    }

    fun sendImage(chatName: String, uri: Uri) {
        val whatsapp = api ?: return
        scope.launch {
            try {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@launch
                val chat = whatsapp.store().findChatByName(chatName).orElse(null) ?: return@launch
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
                    ),
                )
                val image = ImageMessageSimpleBuilder()
                    .media(bytes)
                    .mimeType("image/jpeg")
                    .build()
                whatsapp.sendMessage(chat, image).whenComplete { _, _ ->
                    markStatus(chatName, localId, MessageReadStatus.SENT)
                }
            } catch (_: Throwable) {
                // Best effort: the UI already shows the local bubble.
            }
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

    // ── Disconnect ─────────────────────────────────────────────────────────────

    fun disconnect() {
        scope.launch {
            try {
                api?.disconnect()?.join(10, TimeUnit.SECONDS)
            } catch (_: Throwable) {
                // Ignore: the socket may already be down.
            }
            synchronized(lock) { api = null }
            clearSession()
        }
    }

    private suspend fun clearSession() {
        val store = SessionStore.get(appContext)
        store.setSessionUuid(null)
        store.setSessionPhone(null)
        try {
            File(appContext.filesDir, "cobalt").deleteRecursively()
        } catch (_: Throwable) {
            // Best effort.
        }
        _chats.value = emptyList()
        _messages.value = emptyMap()
        _phoneLinkState.value = PhoneLinkState.Idle
        _connectionState.value = ConnectionStatus.NeedsLink
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun sessionSerializer(): ControllerSerializer {
        val dir = File(appContext.filesDir, "cobalt").apply { mkdirs() }
        val path: Path = dir.toPath()
        return ControllerSerializer.toProtobuf(path)
    }

    private fun saveToCache(name: String, bytes: ByteArray): String? = try {
        val dir = File(appContext.cacheDir, "wa_media").apply { mkdirs() }
        val file = File(dir, name.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".jpg")
        file.writeBytes(bytes)
        file.absolutePath
    } catch (_: Throwable) {
        null
    }
}
