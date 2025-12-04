# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep video player classes
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

