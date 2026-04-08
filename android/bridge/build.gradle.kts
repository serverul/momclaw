plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version Versions.kotlin
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.loa.momclaw.bridge"
    compileSdk = Versions.compileSdk

    defaultConfig {
        minSdk = Versions.minSdk
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
    // TensorFlow Lite
    implementation(Dependencies.TFLite.core)
    implementation(Dependencies.TFLite.support)
    implementation(Dependencies.TFLite.gpu)
    implementation(Dependencies.TFLite.selectOps)

    // Ktor server
    implementation(Dependencies.Ktor.serverNetty)
    implementation(Dependencies.Ktor.serverContentNegotiation)
    implementation(Dependencies.Ktor.serializationKotlinxJson)
    implementation(Dependencies.Ktor.serverCors)
    implementation(Dependencies.Ktor.serverCallLogging)

    // Kotlinx Serialization
    implementation(Dependencies.Kotlinx.serializationJson)

    // Coroutines
    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Coroutines.core)

    // Android Core
    implementation(Dependencies.AndroidX.coreKtx)
    implementation(Dependencies.AndroidX.appcompat)

    // Hilt
    implementation(Dependencies.Hilt.android)
    kapt(Dependencies.Hilt.compiler)

    // Testing
    testImplementation(Dependencies.Test.junit)
    androidTestImplementation(Dependencies.Test.junitExt)
    androidTestImplementation(Dependencies.Test.espressoCore)
}

kapt {
    correctErrorTypes = true
}
