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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.google.errorprone.annotations.Immutable

# AWS -> https://stackoverflow.com/questions/61217946/cant-upload-s3-bucket-from-android-using-cognito
#-keep class com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkConnectionType {*;}

# Keep LiveData class
# https://github.com/android/architecture-components-samples/issues/1088#issuecomment-1629001465
-keep class androidx.lifecycle.LiveData { *; }

# https://github.com/square/okhttp/blob/339732e3a1b78be5d792860109047f68a011b5eb/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro#L11-L14
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# TODO: Waiting for new retrofit release to remove these rules
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keep,allowobfuscation,allowshrinking interface org.literacybridge.tbloaderandroid.api_services.ApiService
-keep,allowobfuscation,allowshrinking class org.literacybridge.tbloaderandroid.api_services.NetworkModule