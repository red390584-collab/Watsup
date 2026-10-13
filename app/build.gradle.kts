plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yourapp.whatsappclone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourapp.whatsappclone"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // JaWa 0.0.4 targets Java 21 (per its POM).
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/index.list",
                "META-INF/io.netty.versions.properties",
                "META-INF/versions/9/module-info.class",
                "META-INF/versions/11/module-info.class",
                "module-info.class",
                "debug.kt_module",
                "main.kt_module"
            )
        }
    }
}

dependencies {
    // JaWa 0.0.4 — pure Java WhatsApp Web bridge (Maven Central).
    // Verified API via: github.com/jochris/JaWa README + source, and
    // javap -public on JaWa-0.0.4.jar (id.jawa.client.JaWaClient etc.).
    implementation("io.github.jochris:JaWa:0.0.4") {
        // JaWa declares com.google.protobuf:protobuf-java, but Android already
        // brings protobuf-javalite. Exclude the heavy protobuf-java so D8 sees
        // only the Android-friendly javalite variant and checkDebugDuplicateClasses
        // passes.
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    // SLF4J binding so JaWa's logging has an Android-backed implementation.
    implementation("org.slf4j:slf4j-android:1.7.36")

    // Protobuf — Android ships protobuf-javalite; keep only that variant.
    // JaWa's protobuf-java is excluded above so D8 does not see both.
    implementation("com.google.protobuf:protobuf-javalite:3.10.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle + navigation
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Persistence (session id, theme, disclaimer acceptance)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // QR generation (encode JaWa's pairing payload for the UI)
    implementation("com.google.zxing:core:3.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
