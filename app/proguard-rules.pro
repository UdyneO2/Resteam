# Keep line numbers so crash reports from release builds are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Backdrop leans on RuntimeShader/AGSL on Android 13+. Keep its public surface
# so R8 doesn't strip effect classes that are only reached through lambdas.
-keep class com.kyant.backdrop.** { *; }
-dontwarn com.kyant.backdrop.**

# Launcher state is read by name from SharedPreferences keys, not reflection,
# so no extra keeps are needed for our own code.
