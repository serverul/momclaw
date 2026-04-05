# Proguard rules for bridge module

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.loa.momclaw.**$$serializer { *; }
-keepclassmembers class com.loa.momclaw.** { *** Companion; }
-keepclasseswithmembers class com.loa.momclaw.** { kotlinx.serialization.KSerializer serializer(...); }

# Logback
-keep class ch.qos.logback.** { *; }
