/**
 * Centralized dependency versions for MOMCLAW
 *
 * This file provides a single source of truth for all dependency versions.
 * When updating a dependency, change the version here and sync across modules.
 */
object Versions {
    // Build
    const val gradle = "8.3.0"
    const val kotlin = "1.9.22"
    const val kotlinSerialization = "1.6.2"
    const val composeCompiler = "1.5.8"

    // Android
    const val compileSdk = 34
    const val minSdk = 26
    const val targetSdk = 34
    const val ndk = "25.2.9519653"
    const val cmake = "3.22.1"

    // Compose
    const val composeBom = "2024.02.00"

    // AndroidX
    const val coreKtx = "1.12.0"
    const val appcompat = "1.6.1"
    const val activityCompose = "1.8.2"
    const val navigationCompose = "2.7.6"
    const val lifecycle = "2.7.0"
    const val datastore = "1.0.0"

    // Room
    const val room = "2.6.1"

    // Hilt
    const val hilt = "2.50"
    const val hiltNavigationCompose = "1.1.0"

    // Networking
    const val okHttp = "4.12.0"

    // TensorFlow Lite
    const val tflite = "2.14.0"
    const val tfliteSupport = "0.4.4"

    // Ktor (Bridge module)
    const val ktor = "2.3.7"

    // Coroutines
    const val coroutines = "1.7.3"

    // Testing
    const val junit = "4.13.2"
    const val junitExt = "1.1.5"
    const val espresso = "3.5.1"
    const val mockitoKotlin = "5.2.1"
}
