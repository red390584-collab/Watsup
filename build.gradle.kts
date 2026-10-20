// AGP 8.6.1 + Kotlin 1.9.24, verified against Gradle 8.7 and the JDK 17 CI
// runner (the WhatsApp bridge itself is a JavaScript asset inside the
// WebView, so no extra dexing constraints apply).
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
