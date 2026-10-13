package com.yourapp.whatsappclone.data.model

/**
 * View-model representation of a WhatsApp chat as shown in the chat list.
 */
data class UiChat(
    val name: String,
    val subtitle: String,
    val preview: String,
    val timestampMillis: Long,
    val unreadCount: Int,
    val isGroup: Boolean,
    val isPinned: Boolean,
    val isMock: Boolean = false,
)

enum class MessageReadStatus { PENDING, SENT, DELIVERED, READ }
