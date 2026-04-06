# MomClaw ProGuard Rules
# Comprehensive rules for all dependencies

# ================================
# Project-specific rules
# ================================

# Keep model classes for serialization
-keep class com.loa.momclaw.data.** { *; }
-keep class com.loa.momclaw.domain.** { *; }
-keep class com.loa.momclaw.ui.** { *; }

# Keep all serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================
# AndroidX & Jetpack
# ================================

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class androidx.room.paging.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Lifecycle
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    @javax.inject.Inject <init>(...);
}

# Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ================================
# Dependency Injection (Hilt)
# ================================

# Hilt
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.Module class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep,allowobfuscation,allowshrinking class com.google.common.util.concurrent.ListenableFuture
-dontwarn com.google.common.util.concurrent.**

# Hilt worker
-keep class * extends androidx.work.Worker {
    @javax.inject.Inject <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ================================
# Networking
# ================================

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keepnames class okhttp3.internal.Internal
-keepnames class okhttp3.internal.http.HttpHeaders

# OkHttp SSE
-keep class okhttp3.sse.** { *; }

# Retrofit (if used in future)
# -keep class retrofit2.** { *; }
# -keepattributes Signature
# -keepattributes Exceptions

# ================================
# Kotlinx Serialization
# ================================

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Kotlinx Serialization companion objects
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializers for project classes
-keep,includedescriptorclasses class com.loa.momclaw.**$$serializer { *; }
-keepclassmembers class com.loa.momclaw.** {
    *** Companion;
}
-keepclasseswithmembers class com.loa.momclaw.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ================================
# Coroutines
# ================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# ================================
# LiteRT-LM (Google AI Edge)
# ================================

-keep class com.google.ai.edge.litert.** { *; }
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litert.**
-dontwarn com.google.ai.edge.litertlm.**

# Keep TensorFlow Lite delegates and runtime
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.delegate.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep GPU delegate if used
-keep class com.google.ai.edge.litert.delegates.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ================================
# Ktor (bridge module)
# ================================

-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.ktor.server.** { *; }
-keep class io.ktor.client.** { *; }

# ================================
# Logging (Logback)
# ================================

-keep class ch.qos.logback.** { *; }
-dontwarn ch.qos.logback.**
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# ================================
# Compose
# ================================

# Keep Compose functions
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep material icons extended
-keep class androidx.compose.material.icons.** { *; }

# ================================
# WorkManager
# ================================

-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker

# ================================
# General Android optimizations
# ================================

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

-assumenosideeffects class java.lang.System {
    public static *** out(...);
    public static *** err(...);
}

# Keep generic signatures & annotations
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*

# Keep source file names and line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable

# Uncomment to disable obfuscation for debugging
# -dontobfuscate

# ================================
# Optimization (consolidated)
# ================================

# Aggressive optimization — 7 passes for release
-optimizationpasses 7
-allowaccessmodification
-repackageclasses 'a'
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable,code/simplification/string,code/inlining/*

# Keep enum classes (needed for serialization)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Compose runtime recomposition
-keepclassmembers class androidx.compose.runtime.** {
    *** Companion;
    *** recompose(...);
}

# Keep Flow/StateFlow methods
-keepclassmembers class kotlinx.coroutines.flow.** {
    <methods>;
}

# Remove unused kotlin-stdlib-js parts
-dontwarn kotlin.js.**
-dontwarn kotlin.reflect.jvm.internal.**

# Keep reflection usage
-keep class kotlin.reflect.** { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
