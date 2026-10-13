# WhatsApp Clone (Kotlin + Jetpack Compose)

A personal-use, self-hosted WhatsApp client for Android, written **100% in Kotlin**
with Jetpack Compose + Material 3. It mimics the WhatsApp mobile UI (chat list,
conversation bubbles, bottom navigation, themes, attachment sheet) and links to a
real WhatsApp account through the unofficial **JaWa** WhatsApp Web bridge
(`io.github.jochris:JaWa:0.0.4`, pure Java, Maven Central).

> ⚠️ **Disclaimer — read this first.**
> This app uses an unofficial, reverse-engineered WhatsApp client (JaWa).
> Using it may breach WhatsApp's Terms of Service and can result in your phone
> number being temporarily or permanently banned from WhatsApp. Use only with a
> number you can afford to lose. You accept full responsibility. A mandatory
> first-launch disclaimer screen enforces this acknowledgment.

## Features

- 🔗 **Link a device** via QR code (scan with WhatsApp → Linked devices) or
  phone number + verification code
- 💬 Real-time text and image messaging through WhatsApp Web
- 🎨 Faithful WhatsApp design system (light + dark themes, exact palette,
  bubble tails, doodle wallpaper)
- 🖼 Photo attachment sheet with live preview in the conversation
- 🔒 Session persistence — the linked session survives app restarts
  (JaWa FileAuthStore + DataStore for the session UUID)
- 🌗 Theme modes: Light, Dark, System — persisted and applied at runtime

## Tech stack

| Layer          | Choice                                                        |
|----------------|---------------------------------------------------------------|
| Language       | Kotlin 1.9.24 (no Java sources)                                |
| UI             | Jetpack Compose + Material 3                                   |
| Architecture   | MVVM (ViewModel + StateFlow + Compose state)                   |
| WhatsApp API   | JaWa 0.0.4 (`id.jawa.client.JaWaClient`)                       |
| Persistence    | DataStore Preferences                                          |
| Images         | Coil                                                           |
| QR rendering   | ZXing core                                                     |
| Build          | AGP 8.6.1, Gradle 8.7, JDK 21, minSdk 26 / target+compile 34   |

## Building

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

CI builds it automatically via GitHub Actions (`.github/workflows/build-apk.yml`)
and uploads it as the `app-debug` artifact on every push to `main`
(or manually via *workflow_dispatch*).

## Project structure

```
app/src/main/java/com/yourapp/whatsappclone/
├── WhatsAppApp.kt            # Application (JaWa session restore on startup)
├── MainActivity.kt           # Single-activity Compose host
├── data/
│   ├── JaWaManager.kt        # JaWa connection + listeners (all IO wrapped)
│   ├── ConnectionStatus.kt   # ConnectionStatus / PhoneLinkState sealed types
│   ├── SessionStore.kt       # DataStore: session uuid, theme, disclaimer
│   └── model/                # UiChat / UiMessage
├── ui/
│   ├── theme/                # WhatsApp palette, typography, theme
│   ├── nav/                  # NavGraph + bottom navigation
│   ├── onboarding/           # Mandatory disclaimer screen
│   ├── link/                 # QR + phone-number linking panels
│   ├── chats/                # Chat list screen + view model
│   ├── conversation/         # Bubbles, input bar, view model
│   ├── settings/             # Theme switcher, disconnect device
│   └── placeholders/         # Updates / Communities / Calls tabs
└── util/                     # QR generation, time formatting
```

## Notes

- JaWa's session files are stored in the app's private files directory
  (`filesDir/jiwa-session`, via `FileAuthStore`), so the linked session is
  removed only by the *Disconnect Device* action in Settings.
- The QR flow is the recommended linking method. The phone-number flow uses
  JaWa's `requestPairingCode` API. **Known limitation:** JaWa 0.0.4 exposes no
  public submit-code endpoint, so the phone-number flow surfaces an explanatory
  error and recommends QR pairing instead.
- Dependency notes: JaWa declares both `protobuf-java` and `protobuf-javalite`
  (only `protobuf-java` ships — its generated classes extend
  `GeneratedMessageV3`), and its desktop logging backend (logback) is excluded
  in favor of `slf4j-simple` so logs land in logcat.
