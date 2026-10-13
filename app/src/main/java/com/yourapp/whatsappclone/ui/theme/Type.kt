package com.yourapp.whatsappclone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * WhatsApp text styles:
 * screen title 19/600, contact name 17/500, chat preview 14/400,
 * message text 14.2/400, timestamp 12/400, section header 13/600 +0.5.
 */
val WaTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
)
