package com.yourapp.whatsappclone.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.whatsappclone.data.CobaltManager
import com.yourapp.whatsappclone.data.model.UiChat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the chat list. Before a device is linked it renders realistic mock
 * data; after linking it mirrors [CobaltManager.chats] in real time.
 */
class ChatListViewModel : ViewModel() {

    private val query = kotlinx.coroutines.flow.MutableStateFlow("")

    val chats: StateFlow<List<UiChat>> =
        combine(CobaltManager.chats, query) { real, filter ->
            val source = if (real.isNotEmpty()) real else mockChats()
            if (filter.isBlank()) source
            else source.filter { it.name.contains(filter, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, mockChats())

    val isLinked: Boolean get() = CobaltManager.isLinked

    fun setQuery(value: String) {
        query.value = value
    }

    companion object {
        fun mockChats(): List<UiChat> {
            val now = System.currentTimeMillis()
            val minute = 60_000L
            return listOf(
                UiChat(
                    name = "Alice Rivera",
                    subtitle = "Hey! Are we still on for tomorrow?",
                    preview = "Hey! Are we still on for tomorrow?",
                    timestampMillis = now - 3 * minute,
                    unreadCount = 2,
                    isGroup = false,
                    isPinned = true,
                    isMock = true,
                ),
                UiChat(
                    name = "Dev Team 🚀",
                    subtitle = "Marco: I pushed the fix, can you pull?",
                    preview = "Marco: I pushed the fix, can you pull?",
                    timestampMillis = now - 18 * minute,
                    unreadCount = 5,
                    isGroup = true,
                    isPinned = true,
                    isMock = true,
                ),
                UiChat(
                    name = "Mom",
                    subtitle = "Call me when you're free ❤️",
                    preview = "Call me when you're free ❤️",
                    timestampMillis = now - 3 * 60 * minute,
                    unreadCount = 0,
                    isGroup = false,
                    isPinned = false,
                    isMock = true,
                ),
                UiChat(
                    name = "Jonas Weber",
                    subtitle = "You: Sounds good, see you then",
                    preview = "You: Sounds good, see you then",
                    timestampMillis = now - 26 * 60 * minute,
                    unreadCount = 0,
                    isGroup = false,
                    isPinned = false,
                    isMock = true,
                ),
                UiChat(
                    name = "Weekend Hiking Club",
                    subtitle = "Sara: Trail opens at 8am on Saturday",
                    preview = "Sara: Trail opens at 8am on Saturday",
                    timestampMillis = now - 2 * 24 * 60 * minute,
                    unreadCount = 12,
                    isGroup = true,
                    isPinned = false,
                    isMock = true,
                ),
                UiChat(
                    name = "Priya Nair",
                    subtitle = "Thanks! 🙏",
                    preview = "Thanks! 🙏",
                    timestampMillis = now - 4 * 24 * 60 * minute,
                    unreadCount = 0,
                    isGroup = false,
                    isPinned = false,
                    isMock = true,
                ),
            )
        }
    }
}
