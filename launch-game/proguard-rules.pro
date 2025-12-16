# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep OpenGL ES classes
-keep class javax.microedition.khronos.** { *; }
-keep class android.opengl.** { *; }

# Keep GameActivity and related classes
-keep class com.example.wechatfriendforgame.** { *; }