# MomClAW Test-suite - AndroidTest Configuration
# This file configures Android instrumentation tests

apply plugin: 'com.android.test'
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-kapt'

android {
    testOptions {
        animationsDisabled = true
        
        // Unit tests are executed against this testOptions
        unitTests.isIncludeAndroidResources = true
        unitTests.returnDefaultValues = true
    }
}

dependencies {
    androidTestImplementation 'androidx.test.ext:junit:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.1.0'
    androidTestImplementation 'androidx.test:runner:1.6.2'
    androidTestImplementation 'androidx.test:rules:1.6.1'
}

// Test runner configuration
android {
    defaultConfig {
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
}
