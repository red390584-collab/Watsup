# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's proguard-android-optimize.txt configuration.

# Cobalt (it.auties.*) is used reflectively by its protobuf serializer.
-keep class it.auties.** { *; }
-keep interface it.auties.** { *; }
-dontwarn it.auties.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn com.aspose.**
-dontwarn com.github.kokorin.jaffree.**
-dontwarn org.slf4j.**
