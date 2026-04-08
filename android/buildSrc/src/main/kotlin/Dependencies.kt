/**
 * Centralized dependency declarations for MOMCLAW
 *
 * Import these objects in any build.gradle.kts file:
 *   import Dependencies.Compose.bom
 *   import Dependencies.Test.junit
 */
object Dependencies {

    object Compose {
        const val bom = "androidx.compose:compose-bom:${Versions.composeBom}"
        const val ui = "androidx.compose.ui:ui"
        const val uiGraphics = "androidx.compose.ui:ui-graphics"
        const val uiToolingPreview = "androidx.compose.ui:ui-tooling-preview"
        const val material3 = "androidx.compose.material3:material3"
        const val materialIconsExtended = "androidx.compose.material:material-icons-extended"
        const val uiTooling = "androidx.compose.ui:ui-tooling"
        const val uiTestManifest = "androidx.compose.ui:ui-test-manifest"
        const val uiTestJunit4 = "androidx.compose.ui:ui-test-junit4"
    }

    object Activity {
        const val compose = "androidx.activity:activity-compose:${Versions.activityCompose}"
    }

    object Navigation {
        const val compose = "androidx.navigation:navigation-compose:${Versions.navigationCompose}"
    }

    object Lifecycle {
        const val viewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}"
        const val runtimeCompose = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.lifecycle}"
        const val runtimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
    }

    object Room {
        const val runtime = "androidx.room:room-runtime:${Versions.room}"
        const val ktx = "androidx.room:room-ktx:${Versions.room}"
        const val compiler = "androidx.room:room-compiler:${Versions.room}"
        const val testing = "androidx.room:room-testing:${Versions.room}"
    }

    object Datastore {
        const val preferences = "androidx.datastore:datastore-preferences:${Versions.datastore}"
    }

    object OkHttp {
        const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
        const val sse = "com.squareup.okhttp3:okhttp-sse:${Versions.okHttp}"
    }

    object Hilt {
        const val android = "com.google.dagger:hilt-android:${Versions.hilt}"
        const val compiler = "com.google.dagger:hilt-compiler:${Versions.hilt}"
        const val navigationCompose = "androidx.hilt:hilt-navigation-compose:${Versions.hiltNavigationCompose}"
    }

    object Coroutines {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    }

    object Kotlinx {
        const val serializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}"
    }

    object TFLite {
        const val core = "org.tensorflow:tensorflow-lite:${Versions.tflite}"
        const val support = "org.tensorflow:tensorflow-lite-support:${Versions.tfliteSupport}"
        const val gpu = "org.tensorflow:tensorflow-lite-gpu:${Versions.tflite}"
        const val selectOps = "org.tensorflow:tensorflow-lite-select-tf-ops:${Versions.tflite}"
    }

    object Ktor {
        const val serverNetty = "io.ktor:ktor-server-netty:${Versions.ktor}"
        const val serverContentNegotiation = "io.ktor:ktor-server-content-negotiation:${Versions.ktor}"
        const val serializationKotlinxJson = "io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}"
        const val serverCors = "io.ktor:ktor-server-cors:${Versions.ktor}"
        const val serverCallLogging = "io.ktor:ktor-server-call-logging:${Versions.ktor}"
    }

    object AndroidX {
        const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
        const val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    }

    object Test {
        const val junit = "junit:junit:${Versions.junit}"
        const val junitExt = "androidx.test.ext:junit:${Versions.junitExt}"
        const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espresso}"
        const val mockitoKotlin = "org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}"
    }
}
