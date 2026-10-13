package com.yourapp.whatsappclone.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Encodes a [String] (the Cobalt pairing payload) into a scannable QR
 * [ImageBitmap] using ZXing's QRCodeWriter. Returns null if encoding fails.
 */
fun qrStringToImageBitmap(content: String, sizePx: Int = 1024): ImageBitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
        )
        // Cobalt pairing payloads are long; use a larger matrix so the QR
        // stays scannable on a phone camera at arm's length.
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        if (matrix.width <= 0 || matrix.height <= 0) return null
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE,
                )
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        android.util.Log.e("QrGenerator", "QR encode failed for payload len=${content.length}: ${e.message}")
        null
    }
}
