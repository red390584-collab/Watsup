package com.yourapp.whatsappclone.ui.link

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourapp.whatsappclone.data.ConnectionStatus
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaPanelDark
import com.yourapp.whatsappclone.ui.theme.WaTealDark
import com.yourapp.whatsappclone.ui.theme.WaWhite
import com.yourapp.whatsappclone.util.qrStringToImageBitmap

/**
 * Renders JaWa's QR pairing payload as an actual scannable QR bitmap
 * (ZXing QRCodeWriter → ImageBitmap), never as text.
 */
@Composable
fun QrCodePanel(status: ConnectionStatus, onRetry: () -> Unit) {
    val colors = com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        when (status) {
            is ConnectionStatus.QrReady -> {
                val payload = status.payload
                val qrBitmap: ImageBitmap? = remember(payload) { qrStringToImageBitmap(payload) }
                // Size the QR box relative to the payload length: longer payloads
                // produce denser QR codes, so give them less canvas to keep modules
                // large enough to scan with a phone camera.
                val boxSize = if (payload.length > 120) 240.dp else 280.dp
                val padding = if (payload.length > 120) 8.dp else 12.dp

                Text(
                    text = "Scan this code with your phone",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Open WhatsApp → Settings → Linked devices → Link a device",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(boxSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WaWhite)
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap,
                            contentDescription = "WhatsApp pairing QR code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = "Could not render QR code (payload ${payload.length})",
                            color = WaPanelDark,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Waiting for scan…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Payload ${payload.length} chars · tap retry if it never appears",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            is ConnectionStatus.Connecting -> {
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(color = WaGreen, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Preparing your session…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            is ConnectionStatus.Linked -> {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Device linked ✓",
                    style = MaterialTheme.typography.titleMedium,
                    color = WaTealDark,
                )
            }

            is ConnectionStatus.Error -> {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Connection failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRetry) {
                    Text("Try again", color = WaGreen)
                }
            }

            is ConnectionStatus.Disconnected -> {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Disconnected: ${status.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRetry) {
                    Text("Retry", color = WaGreen)
                }
            }

            else -> {
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(color = WaGreen, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Starting…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
