# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Add project specific ProGuard rules here.

# Koin
# koin-android / koin-core-viewmodel ship their own consumer rules (incl. -dontwarn org.koin.**).
# Definitions are resolved at compile time (reified get()/module DSL), so Koin classes and
# Module subclasses must not be kept. Note: Fragment classes loaded via KoinFragmentFactory
# (not used in this Compose-only app) would need their own keep rules.
-dontwarn org.koin.**

# Room
# room-runtime ships -keep class * extends androidx.room.RoomDatabase { void <init>(); }, which
# also preserves the generated *_Impl class name that Room loads reflectively. Entities and DAOs
# are referenced directly by those generated classes, so no annotation-based keeps are required.

# Media3 (ExoPlayer)
# Each media3-* AAR ships its own consumer rules covering the reflective entry points.
-dontwarn androidx.media3.**

# Coil 3
# coil-core ships -keep class * extends coil3.target.GenericViewTarget { *; } and
# -dontwarn coil3.PlatformContext.
-dontwarn coil3.**

# OkHttp
-keepattributes Signature
-keepattributes AnnotationDefault
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin Coroutines
# kotlinx-coroutines-core ships consumer rules that keep MainDispatcherFactory and
# CoroutineExceptionHandler (plus the volatile fields), so those keeps are not repeated here.
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    public <init>();
}
-dontwarn kotlinx.coroutines.debug.**
