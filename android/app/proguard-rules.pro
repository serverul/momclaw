# Add project specific ProGuard rules here.

# Keep model classes for serialization
-keep class com.loa.momclaw.data.** { *; }
-keep class com.loa.momclaw.domain.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# OkHttp / SSE
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Ktor (bridge module)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Logback
-keep class ch.qos.logback.** { *; }

# Hilt
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.Module class *
