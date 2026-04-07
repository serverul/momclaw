# ==================================================
# MOMCLAW ProGuard Configuration - Production Ready
# Version: 1.0.0
# ==================================================

# Keep Kotlin Metadata
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt

# =====================
# Kotlinx Serialization
# =====================
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.loa.momclaw.**$$serializer { *; }
-keepclassmembers class com.loa.momclaw.** {
    *** Companion;
}
-keepclasseswithmembers class com.loa.momclaw.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# =====================
# Jetpack Compose
# =====================
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }
-keepclassmembers class androidx.compose.** {
    *** Companion;
}
-dontwarn androidx.compose.**

# =====================
# OkHttp & Okio
# =====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# =====================
# Ktor Server
# =====================
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-dontwarn io.ktor.**

# =====================
# Room Database
# =====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# =====================
# Hilt / Dagger
# =====================
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# =====================
# Lifecycle & ViewModel
# =====================
-keep class androidx.lifecycle.** { *; }
-keep class androidx.savedstate.** { *; }

# =====================
# DataStore
# =====================
-keep class androidx.datastore.** { *; }

# =====================
# Navigation
# =====================
-keep class androidx.navigation.** { *; }

# =====================
# Android Core
# =====================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# =====================
# Coroutines
# =====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# =====================
# Optimization Settings
# =====================

# Enable aggressive optimization
-optimizationpasses 7
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Remove System.out printing
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# =====================
# Obfuscation Settings
# =====================

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =====================
# Warnings
# =====================
-dontwarn java.lang.instrument.Instrumentation
-dontwarn java.lang.invoke.MethodHandle
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
