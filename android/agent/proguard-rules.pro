# ProGuard rules for agent module
-keep class com.loa.momclaw.agent.** { *; }
-keepclassmembers class com.loa.momclaw.agent.** { *; }

# Keep JSON-related methods
-keepclassmembers class * {
    @org.json.* <methods>;
}
