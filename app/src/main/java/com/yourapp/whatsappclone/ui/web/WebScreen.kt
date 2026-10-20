package com.yourapp.whatsappclone.ui.web

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * The whole app after onboarding: a full-screen WebView that loads the
 * official WhatsApp Web client and layers our custom mobile chat UI on top.
 *
 * Hybrid pipeline (all inside the live https://web.whatsapp.com page — we
 * never navigate away, so the linked session survives):
 *
 *  1. The official page renders its own QR / "Link with phone number" flow.
 *     Cookies + DOM storage are enabled so the session persists across app
 *     restarts.
 *  2. On every page finish we inject file:///android_asset/wppconnect-wa.js
 *     (the WPPConnect wa-js UMD bundle, v4.3.0).
 *  3. A poller waits until wa-js is loaded *and* the user is linked (the main
 *     chat DOM is mounted), then calls back into Kotlin.
 *  4. Kotlin injects custom-ui.html / custom-ui.css / custom-ui.js into the
 *     existing DOM (no reload, no navigation — the session stays alive). The
 *     injected CSS hides the official UI only while our custom UI is mounted,
 *     so the official client remains usable as a fallback if injection fails.
 *  5. custom-ui.js talks to WhatsApp through the verified wa-js API
 *     (WPP.chat.list / getMessages / sendTextMessage / markIsRead and
 *     WPP.ev.on('chat.new_message', …)).
 */
private const val WEB_URL = "https://web.whatsapp.com"
private const val TAG = "WaWeb"

/** Desktop Chrome UA so web.whatsapp.com serves the full desktop client. */
private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@Composable
fun WebScreen() {
    val context = LocalContext.current

    val webView = remember {
        createConfiguredWebView(context)
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack()
    }

    AndroidView(
        factory = { webView },
        update = { },
        onRelease = { wv ->
            try {
                wv.stopLoading()
                wv.destroy()
            } catch (t: Throwable) {
                Log.w(TAG, "WebView destroy failed", t)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(context: Context): WebView =
    WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            // Session persistence: cookies + DOM storage keep WhatsApp Web
            // logged in across app restarts.
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            userAgentString = DESKTOP_UA
            // Required so the injected <script src="file:///android_asset/…">
            // can load the WPPConnect bundle from the APK.
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            // setBuiltInZoomControls has no getter, so no Kotlin property exists.
            setBuiltInZoomControls(false)
            displayZoomControls = false
            // Ignore system font scaling so the desktop layout doesn't break.
            textZoom = 100
        }

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(this, true)

        // Surface page JS errors/warnings in logcat for diagnostics.
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "${it.message()} @${it.sourceId()}:${it.lineNumber()}")
                }
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && url.contains("web.whatsapp.com")) {
                    injectBridge(view)
                }
            }
        }

        addJavascriptInterface(Bridge(context, this), "Android")
        loadUrl(WEB_URL)
    }

/**
 * Step 1+2 of the pipeline: load the WPPConnect bundle and poll until the
 * library is present and the user has actually linked (main chat UI mounted).
 * The poll is guarded so page reloads re-arm it, not duplicate it.
 */
private fun injectBridge(view: WebView?) {
    val script = """
        (function () {
          if (window.__waBridgeInjected) { return; }
          window.__waBridgeInjected = true;
          var poll = setInterval(function () {
            try {
              var libReady = !!(window.WPP && WPP.chat && typeof WPP.chat.list === 'function');
              var loggedIn = false;
              try { loggedIn = !!document.querySelector('#pane-side'); } catch (e1) {}
              if (!loggedIn && libReady) {
                try {
                  loggedIn = WPP.whatsapp.ChatStore.getModelsArray().length > 0;
                } catch (e2) {}
              }
              if (libReady && loggedIn) {
                clearInterval(poll);
                window.__waBridgeInjected = 'done';
                if (window.Android) { Android.onWppReady(); }
              }
            } catch (e) { /* keep polling */ }
          }, 1200);
          var s = document.createElement('script');
          s.src = 'file:///android_asset/wppconnect-wa.js';
          s.onerror = function () {
            if (window.Android) { Android.onBridgeError('failed to load wppconnect-wa.js'); }
          };
          document.head.appendChild(s);
        })();
    """.trimIndent()
    view?.evaluateJavascript(script, null)
}

/**
 * Step 4: inject the custom mobile chat UI into the existing DOM. Assets are
 * base64-embedded so no character escaping issues can occur. The official UI
 * is only hidden once our root is successfully in place (the added class
 * triggers the hiding rule in custom-ui.css) and un-hidden again if the
 * script half of the injection throws.
 */
private fun injectCustomUi(webView: WebView, context: Context) {
    val html = context.readAssetText("custom-ui.html")
    val css = context.readAssetText("custom-ui.css")
    val js = context.readAssetText("custom-ui.js")
    fun b64(text: String): String =
        android.util.Base64.encodeToString(text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)

    val script = """
        (function () {
          if (window.__waCustomUiMounted) { return; }
          try {
            function decode(b64) { return decodeURIComponent(escape(atob(b64))); }
            var root = document.createElement('div');
            root.id = 'wa-custom-root';
            root.innerHTML = decode('${b64(html)}');
            document.body.appendChild(root);
            var style = document.createElement('style');
            style.id = 'wa-custom-style';
            style.textContent = decode('${b64(css)}');
            document.head.appendChild(style);
            document.documentElement.classList.add('wa-hybrid-active');
            window.__waCustomUiMounted = true;
            var s = document.createElement('script');
            s.textContent = decode('${b64(js)}');
            document.head.appendChild(s);
          } catch (e) {
            window.__waCustomUiMounted = false;
            document.documentElement.classList.remove('wa-hybrid-active');
            var root2 = document.getElementById('wa-custom-root');
            if (root2 && root2.parentNode) { root2.parentNode.removeChild(root2); }
            if (window.Android) {
              Android.onBridgeError('custom UI injection: ' + (e && e.message ? e.message : e));
            }
          }
        })();
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}

/**
 * Interface exposed to the page as `Android.*`. Callbacks arrive on a
 * WebView-internal thread, so everything is marshaled to the UI thread.
 */
private class Bridge(
    private val context: Context,
    private val webView: WebView,
) {

    /** Called by the injected poller once wa-js is live and the user linked. */
    @android.webkit.JavascriptInterface
    fun onWppReady() {
        Log.i(TAG, "WPP ready + linked — injecting custom UI")
        webView.post { injectCustomUi(webView, context) }
    }

    /** Custom UI log-out: wipe the WebView session, back to the QR screen. */
    @android.webkit.JavascriptInterface
    fun onLogOut() {
        Log.i(TAG, "Log out requested from custom UI")
        webView.post {
            val cookies = CookieManager.getInstance()
            cookies.removeAllCookies { _ ->
                cookies.flush()
                WebStorage.getInstance().deleteAllData()
                webView.clearCache(true)
                webView.clearFormData()
                webView.clearHistory()
                webView.loadUrl(WEB_URL)
            }
        }
    }

    /** Any bridge/injection failure: log and tell the user. */
    @android.webkit.JavascriptInterface
    fun onBridgeError(message: String?) {
        Log.e(TAG, "Bridge error: $message")
        webView.post {
            Toast.makeText(context, "Bridge error: $message", Toast.LENGTH_LONG).show()
        }
    }
}

private fun Context.readAssetText(name: String): String =
    assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
