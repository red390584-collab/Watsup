# Watsup — a WhatsApp client for Android

A personal-use, self-hosted WhatsApp client for Android, written in Kotlin
with Jetpack Compose + Material 3. It uses a **hybrid architecture**: the
*official* WhatsApp Web client runs inside the app's WebView, and once your
device is linked, a custom mobile chat interface takes over the screen while
the official engine keeps running (and keeps your session) underneath.

> ⚠️ **Disclaimer — read this first.**
> This app loads the official WhatsApp Web client inside a custom interface.
> It is an unofficial, personal-use tool, is **not affiliated with WhatsApp or
> Meta**, and linking an account through it may be limited by WhatsApp's
> Terms of Service. You accept full responsibility for how you use it.

## How it works

```
┌──────────────────────────────────────────────────────────┐
│ Android app (Kotlin, single activity)                    │
│  ┌────────────────────────────────────────────────────┐  │
│  │ WebView — https://web.whatsapp.com (desktop mode)  │  │
│  │                                                    │  │
│  │  1. Official QR / phone-number linking screen      │  │
│  │  2. wppconnect-wa.js injected (wa-js v4.3.0)       │  │
│  │  3. After linking: official UI hidden, custom UI   │  │
│  │     injected into the live DOM (no reload)         │  │
│  │                                                    │  │
│  │  custom-ui.js ⇄ WPP.chat.* ⇄ WhatsApp engine       │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

1. **Link** — the WebView loads `https://web.whatsapp.com` with a desktop
   Chrome user agent, cookies and DOM storage enabled. Both official linking
   flows work: scan the QR code, or use *Link with phone number* (the page
   shows the code; enter it on your phone). The WebView is never navigated or
   reloaded during linking, so the session stays alive the whole time.
2. **Bridge** — on every page load the app injects
   `assets/wppconnect-wa.js` (the [WPPConnect wa-js](https://github.com/wppconnect-team/wa-js)
   v4.3.0 UMD bundle). A poller waits until the library is live *and* the
   device is actually linked, then hands control back to Kotlin.
3. **Custom UI** — Kotlin injects `assets/custom-ui.html`, `custom-ui.css`
   and `custom-ui.js` into the existing DOM. The CSS hides the official UI
   only while the custom UI is mounted (if injection ever fails, the official
   client stays visible and fully usable as a fallback).
4. **Chat** — `custom-ui.js` renders every chat from the official engine via
   the wa-js API (`WPP.chat.list()`, `WPP.chat.getMessages()`,
   `WPP.chat.sendTextMessage()`, `WPP.chat.markIsRead()`) and streams live
   updates through `WPP.ev.on('chat.new_message', …)` /
   `'chat.msg_ack_change'` / `'chat.msg_revoke'`.

The session persists across app restarts (WebView cookies + DOM storage);
opening the app while already linked goes straight to your chats.

## Building

CI (GitHub Actions) builds the APK on every push to `main`:

- Workflow: `.github/workflows/build-apk.yml` (JDK 17, `./gradlew assembleDebug`)
- Artifact: **`app-debug`** → `app/build/outputs/apk/debug/app-debug.apk`
  ( downloadable from the run's artifacts section, kept 30 days)

Locally: `./gradlew assembleDebug` (needs JDK 17 and the Android SDK).

## Project layout

```
app/src/main/assets/
  wppconnect-wa.js   # WPPConnect wa-js v4.3.0 (UMD, bridge to the engine)
  custom-ui.html     # custom mobile chat interface markup
  custom-ui.css      # WhatsApp-style theme (light/dark) + official-UI hiding
  custom-ui.js       # renders chats, sends/receives via WPP.chat.*
app/src/main/java/com/yourapp/whatsappclone/
  MainActivity.kt    # theme + one-time disclaimer, then hosts WebScreen
  ui/web/WebScreen.kt# WebView setup, session persistence, JS bridge/injection
  ui/onboarding/     # first-launch disclaimer
  ui/theme/          # Material 3 theme controller
  data/SessionStore.kt # disclaimer + theme persistence (DataStore)
```

## Features

- 🔗 Link via QR code **or** phone number (both official flows, session-safe)
- 💬 Custom mobile chat list + conversation view over the real engine
- ✅ Send/receive text messages, delivery/read ticks, delete-for-everyone
- 🔍 Chat search, unread badges, mark-as-read
- 🌗 Light/dark theme following the system setting
- 🚪 Log out from the custom UI (clears the WebView session)
- ↩️ Android back button closes the open chat, then exits

## Limitations

- Text messaging is fully wired; media is labelled (📷/🎬/🎤/📎) but
  attachments can't be picked from the custom UI yet — send/receive media
  from the official UI if needed.
- The custom UI shows chats already known to the linked session; very large
  chat lists are capped at 300 rows (use search).
- Unofficial usage always carries account risk — see the disclaimer above.
