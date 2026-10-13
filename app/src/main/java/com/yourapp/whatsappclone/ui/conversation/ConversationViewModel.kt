package com.yourapp.whatsappclone.ui.conversation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourapp.whatsappclone.data.CobaltManager
import com.yourapp.whatsappclone.data.model.MessageReadStatus
import com.yourapp.whatsappclone.data.model.UiMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Conversation view model for a single chat. Real messages come from
 * [CobaltManager]; before the device is linked a local demo thread keeps the
 * screen fully interactive.
 */
class ConversationViewModel(private val chatName: String) : ViewModel() {

    private val localExtra = MutableStateFlow<List<UiMessage>>(emptyList())

    val messages: StateFlow<List<UiMessage>> =
        combine(CobaltManager.messages, localExtra) { real, extra ->
            val base = real[chatName] ?: demoMessages(chatName)
            (base + extra).distinctBy { it.id }.sortedBy { it.timestampMillis }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, demoMessages(chatName))

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (CobaltManager.isLinked) {
            CobaltManager.sendText(chatName, trimmed)
            return
        }
        val now = System.currentTimeMillis()
        localExtra.value = localExtra.value + UiMessage(
            id = "local_${now}",
            chatName = chatName,
            text = trimmed,
            timestampMillis = now,
            fromMe = true,
            status = MessageReadStatus.READ,
        )
        viewModelScope.launch {
            delay(1_200)
            localExtra.value = localExtra.value + UiMessage(
                id = "local_${System.currentTimeMillis()}",
                chatName = chatName,
                text = demoReply(trimmed),
                timestampMillis = System.currentTimeMillis(),
                fromMe = false,
                status = MessageReadStatus.SENT,
            )
        }
    }

    fun sendImage(uri: Uri) {
        if (CobaltManager.isLinked) {
            CobaltManager.sendImage(chatName, uri)
            return
        }
        localExtra.value = localExtra.value + UiMessage(
            id = "local_${System.currentTimeMillis()}",
            chatName = chatName,
            text = "",
            timestampMillis = System.currentTimeMillis(),
            fromMe = true,
            status = MessageReadStatus.READ,
            imagePath = uri.toString(),
        )
    }

    companion object {
        fun factory(chatName: String) = viewModelFactory {
            initializer { ConversationViewModel(chatName) }
        }

        private fun demoMessages(chatName: String): List<UiMessage> {
            val now = System.currentTimeMillis()
            val minute = 60_000L
            return listOf(
                UiMessage(
                    id = "demo_1",
                    chatName = chatName,
                    text = "Hey $chatName! 👋 This is a preview thread — link a device " +
                        "from the Link screen to chat for real.",
                    timestampMillis = now - 42 * minute,
                    fromMe = false,
                    status = MessageReadStatus.SENT,
                ),
                UiMessage(
                    id = "demo_2",
                    chatName = chatName,
                    text = "Nice! How does the linking work?",
                    timestampMillis = now - 40 * minute,
                    fromMe = true,
                    status = MessageReadStatus.READ,
                ),
                UiMessage(
                    id = "demo_3",
                    chatName = chatName,
                    text = "Scan the QR code with your phone: WhatsApp → " +
                        "Settings → Linked devices → Link a device.",
                    timestampMillis = now - 39 * minute,
                    fromMe = false,
                    status = MessageReadStatus.SENT,
                ),
                UiMessage(
                    id = "demo_4",
                    chatName = chatName,
                    text = "Perfect, let me try it now.",
                    timestampMillis = now - 38 * minute,
                    fromMe = true,
                    status = MessageReadStatus.DELIVERED,
                ),
            )
        }

        private fun demoReply(input: String): String = when {
            input.contains("?") -> "Good question! I'm a demo reply — link a real device to reach me for real."
            input.contains("thanks", ignoreCase = true) -> "You're welcome! 😊"
            else -> "Got it: \"$input\" — I'm a demo reply shown before linking."
        }
    }
}
