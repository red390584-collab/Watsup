package com.yourapp.whatsappclone.ui.placeholders

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourapp.whatsappclone.ui.nav.Routes
import com.yourapp.whatsappclone.ui.nav.WaBottomBar
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.Muted
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaTeal
import com.yourapp.whatsappclone.ui.theme.WaWhite
import java.util.concurrent.TimeUnit

/** WhatsApp "Updates" tab: status + channels. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(navController: androidx.navigation.NavController) {
    val colors = LocalWaExtendedColors.current
    val now = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Updates",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = colors.textSecondary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = colors.textSecondary)
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
                onClick = { },
                containerColor = colors.topBar,
                contentColor = WaGreen,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New status")
            }
        },
        containerColor = colors.chatBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.topBar)
                        .clickable { }
                        .padding(16.dp),
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .background(WaTeal, CircleShape),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = WaWhite)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .background(WaGreen, CircleShape)
                                .padding(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = WaWhite,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "My status",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Tap to add status update",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            item { SectionLabel("Recent updates", colors.textSecondary) }

            item {
                StatusRow("Alice Rivera", "Today, 09:12", now - TimeUnit.HOURS.toMillis(3), colors.textPrimary, colors.textSecondary)
            }
            item {
                StatusRow("Jonas Weber", "Today, 07:48", now - TimeUnit.HOURS.toMillis(5), colors.textPrimary, colors.textSecondary)
            }

            item { SectionLabel("Channels", colors.textSecondary) }

            item {
                ChannelRow("Android Developers", "New Compose APIs deep dive 🚀", colors.textPrimary, colors.textSecondary)
            }
            item {
                ChannelRow("JetBrains Blog", "Kotlin 1.9.24 is out", colors.textPrimary, colors.textSecondary)
            }
            item {
                ChannelRow("NASA", "Artemis II launch window update", colors.textPrimary, colors.textSecondary)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(title: String, textColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = textColor,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun StatusRow(
    name: String,
    time: String,
    timestampMillis: Long,
    textPrimary: Color,
    textSecondary: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(WaGreen.copy(alpha = 0.85f), CircleShape),
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = WaWhite,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = name, style = MaterialTheme.typography.titleMedium, color = textPrimary)
            Text(text = time, style = MaterialTheme.typography.bodySmall, color = textSecondary)
        }
    }
}

@Composable
private fun ChannelRow(name: String, latest: String, textPrimary: Color, textSecondary: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(WaTeal, RoundedCornerShape(14.dp)),
        ) {
            Icon(Icons.Filled.Campaign, contentDescription = null, tint = WaWhite, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = textPrimary,
                fontWeight = FontWeight.Medium,
            )
            Text(text = latest, style = MaterialTheme.typography.bodySmall, color = textSecondary, maxLines = 1)
        }
    }
}
