package com.yourapp.whatsappclone.ui.placeholders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yourapp.whatsappclone.ui.nav.WaBottomBar
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.Muted
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaTeal
import com.yourapp.whatsappclone.ui.theme.WaWhite

/** WhatsApp "Calls" tab with a recent call log. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(navController: androidx.navigation.NavController) {
    val colors = LocalWaExtendedColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calls",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = colors.textSecondary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.AddIcCall, contentDescription = "New call", tint = colors.textSecondary)
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
        containerColor = colors.chatBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            item {
                CallRow("Mom", "Yesterday, 20:14", outgoing = true, missed = false)
            }
            item {
                CallRow("Alice Rivera", "Yesterday, 18:02", outgoing = false, missed = true)
            }
            item {
                CallRow("Dev Team 🚀", "Tuesday, 11:30", outgoing = true, missed = false)
            }
            item {
                CallRow("Jonas Weber", "Monday, 09:45", outgoing = false, missed = false)
            }
            item {
                CallRow("Priya Nair", "Sunday, 15:21", outgoing = true, missed = false)
            }
        }
    }
}

@Composable
private fun CallRow(name: String, detail: String, outgoing: Boolean, missed: Boolean) {
    val colors = LocalWaExtendedColors.current
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (missed) MaterialTheme.colorScheme.error else colors.textPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (outgoing) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = if (missed) MaterialTheme.colorScheme.error else Muted,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (missed) MaterialTheme.colorScheme.error else colors.textSecondary,
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.Call as ImageVector,
            contentDescription = "Call back",
            tint = WaTeal,
        )
    }
}
