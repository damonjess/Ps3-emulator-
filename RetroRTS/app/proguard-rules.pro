# Starter ProGuard rules for RetroRTS

# Keep the JNI bridge methods so they are not stripped or renamed by R8
-keep class com.retrorts.ui.DosboxBridge {
    native <methods>;
}

# Keep the companion object if necessary (since @JvmStatic methods are there)
-keepclassmembers class com.retrorts.ui.DosboxBridge$Companion {
    native <methods>;
}
