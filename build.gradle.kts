// AGP 8.6.1 (not 8.5.2): the D8 bundled with AGP 8.5.x crashes dexing the Cobalt
// jar with "Type sort is not supported: 9" (SocketHandler invokedynamic with an
// ARRAY bootstrap constant). Verified locally that 8.6.1's D8 dexes it fine.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
