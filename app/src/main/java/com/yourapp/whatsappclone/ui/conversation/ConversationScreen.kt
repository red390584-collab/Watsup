package com.yourapp.whatsappclone.ui.conversation

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourapp.whatsappclone.R
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaWhite

/**
 * One-on-one / group conversation: doodle wallpaper, reverse-layout bubble
 * list and the composer pinned at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatName: String,
    onBack: () -> Unit,
    viewModel: ConversationViewModel = viewModel(
        key = chatName,
        factory = ConversationViewModel.factory(chatName),
    ),
) {
    val colors = LocalWaExtendedColors.current
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxBubbleWidth = 0.78f * screenWidthDp

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(WaGreen.copy(alpha = 0.85f), CircleShape),
                        ) {
                            Text(
                                text = chatName.trim().firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = WaWhite,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = chatName,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "online",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBar,
                    navigationIconContentColor = colors.textSecondary,
                ),
            )
        },
        bottomBar = {
            InputBar(
                onSendText = viewModel::send,
                onSendImage = viewModel::sendImage,
            )
        },
        containerColor = colors.chatBackground,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.chatBackground),
        ) {
            Image(
                painter = painterResource(id = R.drawable.wallpaper_default),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.05f,
                modifier = Modifier.fillMaxSize(),
            )

            LazyColumn(
                state = listState,
                reverseLayout = true,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
            ) {
                items(
                    items = messages.asReversed(),
                    key = { it.id },
                ) { message ->
                    MessageBubble(
                        message = message,
                        maxBubbleWidth = maxBubbleWidth.dp,
                        showSender = false,
                    )
                }
            }
        }
    }
}
