package com.yourapp.whatsappclone.ui.conversation

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.Muted
import com.yourapp.whatsappclone.ui.theme.WaGreen
import com.yourapp.whatsappclone.ui.theme.WaWhite
import java.io.File

/**
 * WhatsApp-style composer: rounded 24dp input surface, camera + paperclip
 * icons and a green circular send button (shown once text is typed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputBar(
    onSendText: (String) -> Unit,
    onSendImage: (Uri) -> Unit,
) {
    val colors = LocalWaExtendedColors.current
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(onSendImage)
    }

    val cameraCapture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) pendingCaptureUri?.let(onSendImage)
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createCaptureUri(context)
            pendingCaptureUri = uri
            uri?.let { cameraCapture.launch(it) }
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = colors.topBar,
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                SheetRow(
                    icon = { Icon(Icons.Filled.Image, contentDescription = null, tint = WaGreen) },
                    label = "Photo & video",
                    onClick = {
                        showSheet = false
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                SheetRow(
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = WaGreen) },
                    label = "Camera",
                    onClick = {
                        showSheet = false
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    },
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.inputBar)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .background(colors.searchChip, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach",
                        tint = colors.textSecondary,
                    )
                }
                Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Type a message",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted,
                            modifier = Modifier.padding(vertical = 15.dp),
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary,
                        ),
                        cursorBrush = SolidColor(WaGreen),
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    )
                }
                IconButton(onClick = {
                    showSheet = false
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Camera",
                        tint = colors.textSecondary,
                    )
                }
            }
        }

        if (text.isBlank()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(colors.inputBar),
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice message",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
        } else {
            FloatingActionButton(
                onClick = {
                    val message = text
                    text = ""
                    onSendText(message)
                },
                containerColor = WaGreen,
                contentColor = WaWhite,
                shape = CircleShape,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                ),
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    val colors = LocalWaExtendedColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        icon()
        Box(modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
        )
    }
}

private fun createCaptureUri(context: android.content.Context): Uri? = try {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (_: Exception) {
    null
}
