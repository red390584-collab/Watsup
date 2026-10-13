package com.yourapp.whatsappclone.data.model

/**
 * View-model representation of a single chat message. [imagePath] points at a
 * local cache file when the message carries a photo (sent or received).
 */
data class UiMessage(
    val id: String,
    val chatName: String,
    val text: String,
    val timestampMillis: Long,
    val fromMe: Boolean,
    val status: MessageReadStatus = MessageReadStatus.SENT,
    val senderName: String? = null,
    val imagePath: String? = null,
)
