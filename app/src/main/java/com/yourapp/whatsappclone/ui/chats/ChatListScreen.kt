package com.yourapp.whatsappclone.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourapp.whatsappclone.data.model.UiChat
import com.yourapp.whatsappclone.ui.nav.Routes
import com.yourapp.whatsappclone.ui.nav.WaBottomBar
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.Muted
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaWhite
import com.yourapp.whatsappclone.util.TimeFormat
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    onOpenChat: (String) -> Unit,
    viewModel: ChatListViewModel = viewModel(),
) {
    val colors = LocalWaExtendedColors.current
    val chats by viewModel.chats.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var showNewChat by remember { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var pinned by remember { mutableStateOf(chats.filter { it.isPinned }) }
    var others by remember { mutableStateOf(chats.filterNot { it.isPinned }) }
    pinned = chats.filter { it.isPinned }
    others = chats.filterNot { it.isPinned }

    if (showNewChat) {
        NewChatDialog(
            onDismiss = { showNewChat = false },
            onStart = { name ->
                showNewChat = false
                if (name.isNotBlank()) onOpenChat(name.trim())
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WhatsApp",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = colors.textSecondary)
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = colors.textSecondary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuOpen = false
                                navController.navigate(Routes.SETTINGS)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Linked devices") },
                            onClick = {
                                menuOpen = false
                                navController.navigate(Routes.LINK)
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBar,
                    titleContentColor = colors.textPrimary,
                    actionIconContentColor = colors.textSecondary,
                ),
            )
        },
        bottomBar = { WaBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChat = true },
                containerColor = WaGreen,
                contentColor = WaWhite,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.AddComment, contentDescription = "New chat")
            }
        },
        containerColor = colors.chatBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.setQuery(it)
                    },
                    placeholder = { Text("Search…", color = Muted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WaGreen,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = WaGreen,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (pinned.isNotEmpty()) {
                    item { SectionHeader("Pinned", colors.textSecondary) }
                    items(pinned, key = { "pin_${it.name}" }) { chat ->
                        ChatRow(chat = chat, onClick = { onOpenChat(chat.name) })
                    }
                }
                if (others.isNotEmpty()) {
                    if (pinned.isNotEmpty()) {
                        item { SectionHeader("Chats", colors.textSecondary) }
                    }
                    items(others, key = { it.name }) { chat ->
                        ChatRow(chat = chat, onClick = { onOpenChat(chat.name) })
                    }
                }
                if (chats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp, start = 32.dp, end = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No chats yet. Tap the button below to start one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, textColor: Color) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun ChatRow(chat: UiChat, onClick: () -> Unit) {
    val colors = LocalWaExtendedColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(colors.topBar)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Avatar(name = chat.name, isGroup = chat.isGroup)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = true),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TimeFormat.short(chat.timestampMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (chat.unreadCount > 0) WaGreen else colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = true),
                )
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .background(WaGreen, CircleShape),
                    ) {
                        Text(
                            text = chat.unreadCount.toString().let { if (chat.unreadCount > 99) "99+" else it },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = WaWhite,
                        )
                    }
                }
            }
        }
    }
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color = colors.divider,
    )
}

@Composable
private fun Avatar(name: String, isGroup: Boolean) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .background(WaGreen.copy(alpha = 0.85f), CircleShape),
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = WaWhite,
        )
    }
}

@Composable
private fun NewChatDialog(onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New chat", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Contact name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WaGreen,
                    cursorColor = WaGreen,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onStart(name) }, enabled = name.isNotBlank()) {
                Text("Start chat", color = WaGreen, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Muted)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
