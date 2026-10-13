package com.yourapp.whatsappclone.ui.placeholders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourapp.whatsappclone.ui.nav.WaBottomBar
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaTeal
import com.yourapp.whatsappclone.ui.theme.WaWhite

/** WhatsApp "Communities" tab: intro screen for creating a community. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(navController: androidx.navigation.NavController) {
    val colors = LocalWaExtendedColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Communities",
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
        containerColor = colors.chatBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .background(WaTeal.copy(alpha = 0.14f), RoundedCornerShape(32.dp)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = WaTeal,
                    modifier = Modifier.size(64.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Stay connected with your communities",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Communities bring related groups together in one place — " +
                    "for a neighborhood, school or team. Announcements reach everyone " +
                    "at once, and membership is admin-approved.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = WaGreen, contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = "New community",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Link a device to bring your existing groups into a community.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
