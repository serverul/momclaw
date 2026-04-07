plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.loa.momclaw.bridge"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    // ============================================================
    // LiteRT-LM SDK (Google AI Edge)
    // ============================================================
    // Status: Placeholder - official SDK not yet publicly available
    //
    // The stub implementations in com.google.ai.edge.litertlm package
    // provide build compatibility. Actual inference requires:
    //
    // Option 1: Wait for official Google SDK
    //   - Monitor: https://ai.google.dev/edge/litert
    //   - Expected artifact: com.google.ai.edge:litert-lm:x.x.x
    //
    // Option 2: Use TensorFlow Lite directly
    //   - implementation("org.tensorflow:tensorflow-lite:2.14.0")
    //   - implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")
    //   - Requires custom model conversion to TFLite format
    //
    // Option 3: Use ML Kit APIs as alternative
    //   - implementation("com.google.mlkit:common:18.10.0")
    //   - Limited to specific tasks (translation, text recognition)
    //
    // Current state: Stub implementations allow UI/API testing
    // Uncomment below when official SDK becomes available:
    // implementation("com.google.ai.edge:litert-lm:1.0.0")
    // ============================================================

    // Ktor server
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-server-cors:2.3.7")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")

    // Logging
    implementation("io.ktor:ktor-server-call-logging:2.3.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

kapt {
    correctErrorTypes = true
}
