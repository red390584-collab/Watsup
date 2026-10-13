# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's proguard-android-optimize.txt configuration.

# Keep the WhatsApp bridge and its protobuf runtime intact if minification is
# ever enabled (release builds currently set isMinifyEnabled = false).
-keep class id.jawa.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn id.jawa.**
-dontwarn org.slf4j.**
# Optional JVM-only classes referenced by transitive jars; absent on Android.
-dontwarn java.awt.**
-dontwarn javax.imageio.**
