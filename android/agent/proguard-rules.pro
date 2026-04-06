# ProGuard rules for agent module

# ================================
# Agent Core
# ================================

-keep class com.loa.momclaw.agent.** { *; }
-keepclassmembers class com.loa.momclaw.agent.** { *; }

# Keep JSON-related methods
-keepclassmembers class * {
    @org.json.* <methods>;
}

# ================================
# Serialization
# ================================

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.loa.momclaw.agent.**$$serializer { *; }
-keepclassmembers class com.loa.momclaw.agent.** {
    *** Companion;
}
-keepclasseswithmembers class com.loa.momclaw.agent.** {
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
# NullClaw Native Integration
# ================================

# Keep native method declarations
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI callback methods
-keepclassmembers class com.loa.momclaw.agent.** {
    public void on*(...);
    public static void on*(...);
}

# Keep NullClaw bridge classes
-keep class com.loa.momclaw.agent.nullclaw.** { *; }
-keepclassmembers class com.loa.momclaw.agent.nullclaw.** { *; }

# ================================
# Inference Engine
# ================================

# Keep model loader classes
-keep class com.loa.momclaw.agent.inference.** { *; }
-keepclassmembers class com.loa.momclaw.agent.inference.** {
    public <methods>;
    protected <methods>;
}

# Keep model config classes
-keep class com.loa.momclaw.agent.config.** { *; }

# ================================
# Logging
# ================================

-keep class io.github.microutils.** { *; }
-dontwarn io.github.microutils.**

# Remove debug logs in release
-assumenosideeffects class kotlin.jvm.functions.Function0 {
    public *** invoke();
}
