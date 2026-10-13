package com.yourapp.whatsappclone.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.yourapp.whatsappclone.data.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun from(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

/**
 * WhatsApp-specific colors that Material 3's ColorScheme cannot express
 * (bubble colors, top/input bar surfaces). Provided via CompositionLocal so
 * any composable can pick the correct token for the active theme.
 */
data class WaExtendedColors(
    val isDark: Boolean,
    val topBar: Color,
    val inputBar: Color,
    val chatBackground: Color,
    val bubbleSent: Color,
    val bubbleReceived: Color,
    val bubbleTextSent: Color,
    val bubbleTextReceived: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val statusBar: Color,
    val navigationBar: Color,
    val badgeUnread: Color,
    val searchChip: Color,
)

val LocalWaExtendedColors = staticCompositionLocalOf { lightWaExtendedColors() }

fun lightWaExtendedColors() = WaExtendedColors(
    isDark = false,
    topBar = WaWhite,
    inputBar = WaWhite,
    chatBackground = BackgroundLight,
    bubbleSent = BubbleSentLight,
    bubbleReceived = BubbleReceivedLight,
    bubbleTextSent = BubbleTextSentLight,
    bubbleTextReceived = Ink,
    textPrimary = Ink,
    textSecondary = Muted,
    divider = Border,
    statusBar = LightStatusBar,
    navigationBar = WaWhite,
    badgeUnread = WaGreen,
    searchChip = BackgroundLight,
)

fun darkWaExtendedColors() = WaExtendedColors(
    isDark = true,
    topBar = WaPanelDarkAlt,
    inputBar = WaPanelDarkAlt,
    chatBackground = BackgroundDark,
    bubbleSent = BubbleSentDark,
    bubbleReceived = BubbleReceivedDark,
    bubbleTextSent = BubbleTextSentDark,
    bubbleTextReceived = InkDark,
    textPrimary = InkDark,
    textSecondary = MutedDark,
    divider = BorderDark,
    statusBar = DarkStatusBar,
    navigationBar = WaPanelDarkAlt,
    badgeUnread = WaGreen,
    searchChip = WaPanelDarkAlt,
)

private val waLightColorScheme = lightColorScheme(
    primary = WaGreen,
    onPrimary = WaWhite,
    primaryContainer = WaGreen,
    onPrimaryContainer = WaWhite,
    secondary = WaTeal,
    onSecondary = WaWhite,
    tertiary = WaTealDark,
    onTertiary = WaWhite,
    background = BackgroundLight,
    onBackground = Ink,
    surface = WaWhite,
    onSurface = Ink,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = Muted,
    surfaceTint = WaGreen,
    outline = Border,
    outlineVariant = Border,
    error = Color(0xFFBA1A1A),
    onError = WaWhite,
    inverseSurface = WaPanelDarkAlt,
    inverseOnSurface = WaWhite,
    inversePrimary = WaGreen,
)

private val waDarkColorScheme = darkColorScheme(
    primary = WaGreen,
    onPrimary = Color(0xFF00391F),
    primaryContainer = WaGreen,
    onPrimaryContainer = WaWhite,
    secondary = WaTeal,
    onSecondary = WaWhite,
    tertiary = WaTealDark,
    onTertiary = WaWhite,
    background = WaPanelDark,
    onBackground = InkDark,
    surface = WaPanelDark,
    onSurface = InkDark,
    surfaceVariant = WaPanelDarkAlt,
    onSurfaceVariant = MutedDark,
    surfaceTint = WaGreen,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    inverseSurface = InkDark,
    inverseOnSurface = WaPanelDarkAlt,
    inversePrimary = WaTeal,
)

/**
 * Global theme controller: the selected [ThemeMode] survives recomposition,
 * is persisted to DataStore and is observed by MainActivity.
 */
object ThemeController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode

    fun load(context: Context) {
        scope.launch {
            _mode.value = ThemeMode.from(SessionStore.get(context).themeMode())
        }
    }

    fun set(context: Context, mode: ThemeMode) {
        _mode.value = mode
        scope.launch { SessionStore.get(context).setThemeMode(mode.name) }
    }
}

@Composable
fun WaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val extended = if (dark) darkWaExtendedColors() else lightWaExtendedColors()
    MaterialTheme(
        colorScheme = if (dark) waDarkColorScheme else waLightColorScheme,
        typography = WaTypography,
        content = {
            CompositionLocalProvider(LocalWaExtendedColors provides extended) {
                content()
            }
        },
    )
}
