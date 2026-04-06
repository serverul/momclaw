# Proguard rules for bridge module

# ================================
# Ktor Server & Client
# ================================

-keep class io.ktor.** { *; }
-keep class io.ktor.server.** { *; }
-keep class io.ktor.client.** { *; }
-dontwarn io.ktor.**

# Keep Ktor application config
-keep class io.ktor.server.config.** { *; }
-keepclassmembers class io.ktor.server.config.** { *; }

# Keep Netty engine
-keep class io.ktor.server.netty.** { *; }
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# ================================
# Kotlinx Serialization
# ================================

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

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

# ================================
# Logging
# ================================

-keep class ch.qos.logback.** { *; }
-dontwarn ch.qos.logback.**

-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# ================================
# Coroutines & Networking
# ================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# Keep OkHttp if used
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ================================
# Bridge-specific optimizations
# ================================

# Keep all REST endpoint methods
-keepclassmembers class com.loa.momclaw.bridge.** {
    @io.ktor.server.routing.** <methods>;
    @io.ktor.http.** <methods>;
}

# Optimize HTTP content negotiation
-keep class io.ktor.serialization.kotlinx.** { *; }

# Remove debug logs in release
-assumenosideeffects class io.ktor.server.logger.** {
    public <methods>;
}

# Keep JSON-related methods
-keepclassmembers class * {
    @org.json.* <methods>;
}

# Keep all bridge module classes
-keep class com.loa.momclaw.bridge.** { *; }
-keepclassmembers class com.loa.momclaw.bridge.** { *; }
