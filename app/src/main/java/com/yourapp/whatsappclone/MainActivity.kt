package com.yourapp.whatsappclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.yourapp.whatsappclone.data.SessionStore
import com.yourapp.whatsappclone.ui.onboarding.DisclaimerScreen
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.ThemeController
import com.yourapp.whatsappclone.ui.theme.WaTheme
import com.yourapp.whatsappclone.ui.web.WebScreen

/**
 * Single-activity host.
 *
 * Flow: first launch shows the one-time disclaimer; afterwards the app is a
 * full-screen [WebScreen] — a WebView that loads the *official*
 * https://web.whatsapp.com, keeps the session alive (cookies + DOM storage)
 * and, once the user has linked via QR or phone number, injects the WPPConnect
 * bridge plus our custom mobile chat UI on top of the live page.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.load(this)
        setContent {
            val mode by ThemeController.mode.collectAsState()
            WaTheme(mode = mode) {
                val extended = LocalWaExtendedColors.current
                SideEffect {
                    val window = this@MainActivity.window
                    @Suppress("DEPRECATION")
                    window.statusBarColor = extended.statusBar.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = extended.navigationBar.toArgb()
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !extended.isDark
                    controller.isAppearanceLightNavigationBars = !extended.isDark
                }
                DisclaimerGate()
            }
        }
    }
}

/**
 * Resolves the persisted disclaimer acceptance once, without flashing the
 * disclaimer at returning users, then keeps the app on the WebView screen.
 */
@Composable
private fun DisclaimerGate() {
    val context = LocalContext.current
    val stored by produceState<Boolean?>(initialValue = null) {
        value = SessionStore.get(context).disclaimerAccepted()
    }
    var acceptedJustNow by remember { mutableStateOf(false) }

    when {
        stored == null -> {
            // Brief splash while DataStore is read; matches the theme background.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
            )
        }
        stored == true || acceptedJustNow -> WebScreen()
        else -> DisclaimerScreen(onAccepted = { acceptedJustNow = true })
    }
}
