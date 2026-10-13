package com.yourapp.whatsappclone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.yourapp.whatsappclone.ui.nav.AppRoot
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.ThemeController
import com.yourapp.whatsappclone.ui.theme.WaTheme

/**
 * Single-activity Compose host. Applies the persisted theme, keeps the system
 * bars in sync with it and requests the notification permission on API 33+.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.load(this)
        maybeRequestNotificationPermission()
        setContent {
            val mode by ThemeController.mode.collectAsState()
            WaTheme(mode = mode) {
                val extended = LocalWaExtendedColors.current
                val activity = LocalContext.current as? MainActivity
                SideEffect {
                    val window = activity?.window ?: return@SideEffect
                    @Suppress("DEPRECATION")
                    window.statusBarColor = extended.statusBar.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = extended.navigationBar.toArgb()
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !extended.isDark
                    controller.isAppearanceLightNavigationBars = !extended.isDark
                }
                AppRoot()
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
