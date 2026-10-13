package com.yourapp.whatsappclone.ui.link

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yourapp.whatsappclone.R
import com.yourapp.whatsappclone.data.CobaltManager
import com.yourapp.whatsappclone.data.ConnectionStatus
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.WaGreen

/**
 * Device-linking screen with the two official flows: QR code (recommended)
 * and phone number + verification code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkDeviceScreen(onLinked: () -> Unit) {
    val colors = LocalWaExtendedColors.current
    val connectionState by CobaltManager.connectionState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        CobaltManager.startQrLinking()
    }
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionStatus.Linked) onLinked()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.link_device),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onLinked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBar,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textSecondary,
                ),
            )
        },
        containerColor = colors.chatBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.topBar,
                contentColor = WaGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = WaGreen,
                    )
                },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selectedTab == 0) WaGreen else colors.textSecondary,
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Phone number",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selectedTab == 1) WaGreen else colors.textSecondary,
                        )
                    },
                )
            }

            when (selectedTab) {
                0 -> QrCodePanel(status = connectionState, onRetry = { CobaltManager.startQrLinking() })
                else -> PhoneLinkPanel()
            }
        }
    }
}
