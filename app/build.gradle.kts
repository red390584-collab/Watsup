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
        // JaWa's POM declares BOTH protobuf-java and protobuf-javalite, and
        // signal-protocol-java (transitive) pulls javalite again — the two jars
        // ship overlapping com.google.protobuf.* classes, so only one may ship
        // in an APK (D8 fails with "Duplicate class" otherwise). JaWa's generated
        // Wa* classes extend com.google.protobuf.GeneratedMessageV3 (full
        // protobuf), and the full protobuf-java jar also contains the lite
        // runtime classes (GeneratedMessageLite) that signal-protocol-java needs.
        // Verified with `javap -classpath JaWa-0.0.4.jar 'id.jawa.proto.Wa$Message'`.
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")

        // Logback is a desktop-only SLF4J backend: JaWa's own code never
        // references ch.qos.logback classes (scanned all 2466 classes in the
        // jar — zero hits; only org.slf4j), and logback-classic + logback-core
        // both ship META-INF/INDEX.LIST, which fails :app:mergeDebugJavaResource
        // with "2 files found with path META-INF/INDEX.LIST". slf4j-simple below
        // is the Android-safe 2.x provider instead.
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "ch.qos.logback", module = "logback-core")
    }

    // SLF4J provider for JaWa (slf4j-api 2.0.13 arrives transitively from
    // JaWa). slf4j-simple 2.0.x is a real slf4j-api-2.x provider that only
    // writes to System.err → visible in logcat. Note: slf4j-android:1.7.36 is a
    // 1.7 StaticLoggerBinder binding and would be inert under slf4j-api 2.x.
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // Full protobuf runtime (3.10.0 = JaWa's protobuf.version). JaWa brings it
    // transitively; pin it explicitly and keep javalite out (excluded above).
    implementation("com.google.protobuf:protobuf-java:3.10.0")

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
