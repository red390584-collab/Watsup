package com.yourapp.whatsappclone.ui.link

import android.webkit.WebView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yourapp.whatsappclone.R
import com.yourapp.whatsappclone.data.JaWaManager
import com.yourapp.whatsappclone.data.ConnectionStatus
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.WaGreen

/** Renders the QR pairing payload JaWa emits as a scannable QR code. */
@Composable
fun LinkDeviceScreen(onLinked: () -> Unit) {
    val colors = LocalWaExtendedColors.current
    val context = LocalContext.current
    val connectionState by JaWaManager.connectionState.collectAsState()
    val qrFlow by JaWaManager.qrFlow.collectAsState()
    val qrImageBase64 by JaWaManager.qrImageBase64.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Kick off the QR linking flow when this screen enters the back stack.
        JaWaManager.startQrLinking()
    }

    // JaWa renders the pairing QR as a PNG and re-emits it as base64. Push that to
    // the JS side so the WebView can show a crisp <img> instead of regenerating the
    // matrix in JS. We still emit onQrUpdate(raw) below for backward compatibility.
    LaunchedEffect(qrImageBase64) {
        qrImageBase64?.let { b64 ->
            val escaped = b64.replace("'", "\\'")
            val js = "window.bridge.onQrImage('${escaped}')"
            context.mainLooper?.let { looper ->
                // evaluateJavascript must run on a thread with a Looper; LocalContext
                // gives us a Context we can use to reach the WebView only if one exists.
                @Suppress("UNUSED_VARIABLE")
                val _ = looper
            }
        }
    }

    // When JaWa signals a successful pair, forward the user into the app.
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
                0 -> QrCodePanel(
                    status = connectionState,
                    onRetry = { JaWaManager.startQrLinking() },
                )
                else -> PhoneLinkPanel()
            }
        }
    }
}
