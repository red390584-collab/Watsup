package com.yourapp.whatsappclone.ui.conversation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yourapp.whatsappclone.data.model.MessageReadStatus
import com.yourapp.whatsappclone.data.model.UiMessage
import com.yourapp.whatsappclone.ui.theme.CheckBlue
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.Muted
import com.yourapp.whatsappclone.util.TimeFormat
import java.io.File

/**
 * A single chat bubble: rounded 8dp surface, small triangular Canvas tail on
 * the sent side, text/image content, timestamp and read-receipt checks.
 */
@Composable
fun MessageBubble(
    message: UiMessage,
    maxBubbleWidth: Dp,
    showSender: Boolean,
) {
    val colors = LocalWaExtendedColors.current
    val bubbleColor = if (message.fromMe) colors.bubbleSent else colors.bubbleReceived
    val textColor = if (message.fromMe) colors.bubbleTextSent else colors.bubbleTextReceived

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (!message.fromMe) {
            BubbleTail(color = bubbleColor, flipped = true)
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.widthIn(max = maxBubbleWidth),
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (showSender && !message.fromMe && message.senderName != null) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.8.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        ),
                        color = CheckBlue,
                    )
                }

                val imagePath = message.imagePath
                if (imagePath != null) {
                    val model: Any = if (imagePath.startsWith("/")) File(imagePath) else android.net.Uri.parse(imagePath)
                    AsyncImage(
                        model = model,
                        contentDescription = "Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.2.sp),
                        color = textColor,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(
                        text = TimeFormat.clockOnly(message.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.fromMe) textColor.copy(alpha = 0.6f) else colors.textSecondary,
                    )
                    if (message.fromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        ReadReceipt(status = message.status)
                    }
                }
            }
        }

        if (message.fromMe) {
            BubbleTail(color = bubbleColor, flipped = false)
        }
    }
}

/** The classic WhatsApp double-check indicator. */
@Composable
private fun ReadReceipt(status: MessageReadStatus) {
    when (status) {
        MessageReadStatus.PENDING -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Pending",
            tint = Muted,
            modifier = Modifier.size(14.dp),
        )
        MessageReadStatus.SENT -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Sent",
            tint = Muted,
            modifier = Modifier.size(14.dp),
        )
        MessageReadStatus.DELIVERED -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Delivered",
            tint = Muted,
            modifier = Modifier.size(14.dp),
        )
        MessageReadStatus.READ -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Read",
            tint = CheckBlue,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** Small triangular tail drawn with a Canvas overlay on the bubble side. */
@Composable
private fun BubbleTail(color: Color, flipped: Boolean) {
    Canvas(
        modifier = Modifier
            .size(width = 8.dp, height = 12.dp),
    ) {
        val path = Path().apply {
            if (flipped) {
                moveTo(size.width, 0f)
                lineTo(0f, 0f)
                lineTo(size.width, size.height * 0.55f)
                close()
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height * 0.55f)
                close()
            }
        }
        drawPath(path = path, color = color)
    }
}
