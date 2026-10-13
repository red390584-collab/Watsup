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
fun qrStringToImageBitmap(content: String, sizePx: Int = 720): ImageBitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
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
    } catch (_: Exception) {
        null
    }
}
