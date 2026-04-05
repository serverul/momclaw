# Consumer rules for bridge module
# These rules are applied to apps that consume the bridge library

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.loa.momclaw.bridge.**$$serializer { *; }
-keepclassmembers class com.loa.momclaw.bridge.** { *** Companion; }
-keepclasseswithmembers class com.loa.momclaw.bridge.** { kotlinx.serialization.KSerializer serializer(...); }

# LiteRT-LM
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**

# Logback
-keep class ch.qos.logback.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
