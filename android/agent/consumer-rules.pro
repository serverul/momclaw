# Consumer rules for agent module
# These rules are applied to apps that consume the agent library

# Keep all agent classes
-keep class com.loa.momclaw.agent.** { *; }
-keepclassmembers class com.loa.momclaw.agent.** { *; }

# Keep JSON-related methods
-keepclassmembers class * {
    @org.json.* <methods>;
}

# Keep NullClaw bridge interfaces
-keep interface com.loa.momclaw.agent.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
