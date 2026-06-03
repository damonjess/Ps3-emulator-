# Keep JNI bridge classes — R8 must not rename or strip these
-keep class com.retrorts.ui.DosboxBridge {
    native <methods>;
}
-keepclassmembers class com.retrorts.ui.DosboxBridge$Companion {
    native <methods>;
}

-keep class com.retrorts.ui.NativeEmulatorBridge {
    native <methods>;
    public static *** setSurface(...);
    public static *** launchGame(...);
}

# Keep GameProfile JSON serialisation (uses org.json reflection)
-keepclassmembers class com.retrorts.ui.GameProfile {
    public <init>(...);
    public *** toJson();
    public static *** fromJson(...);
}

# Keep PerfStats data class
-keep class com.retrorts.ui.PerfStats { *; }
