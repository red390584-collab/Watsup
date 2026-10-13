// AGP 8.6.1 (not 8.5.2): 8.5.2's D8 crashes dexing JVM-only WhatsApp bridge
// jars with "Type sort is not supported: 9" (invokedynamic with an ARRAY
// bootstrap constant). Verified that 8.6.1's D8 dexes them fine.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
