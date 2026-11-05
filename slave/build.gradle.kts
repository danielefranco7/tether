plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "it.nuovetech.tether.slave"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.nuovetech.tether.slave"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Sviluppo: stessi valori della Master (vedi master/build.gradle.kts).
        buildConfigField("String", "RELAY_URL", "\"${project.findProperty("relayUrl") ?: "ws://10.0.2.2:8080"}\"")
        buildConfigField("String", "CONVERSATION_KEY", "\"${project.findProperty("conversationKey") ?: "dev-room"}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Generazione QR per il pairing (puro Java, nessun Play Services).
    implementation("com.google.zxing:core:3.5.3")
}
