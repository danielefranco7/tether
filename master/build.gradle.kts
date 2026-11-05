plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "it.nuovetech.tether.master"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.nuovetech.tether.master"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Sviluppo: URL del relay e stanza di test. Per un telefono fisico impostare relayUrl
        // in gradle.properties con l'IP del PC (es. ws://192.168.1.20:8080).
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
}
