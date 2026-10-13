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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    // Cobalt: unofficial WhatsApp Web API (JVM). Aspose is excluded because it is
    // only hosted on Aspose's own maven repo and is not needed for messaging.
    implementation("com.github.auties00:cobalt:0.0.9") {
        exclude(group = "com.aspose", module = "aspose-words")
        exclude(group = "com.github.kokorin.jaffree", module = "jaffree")
    }

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

    // QR generation (encode)
    implementation("com.google.zxing:core:3.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
