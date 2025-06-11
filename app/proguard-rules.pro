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

# Keep ContactsHelper methods
-keep class com.safecallkids.app.ContactsHelper { *; }

# Keep broadcast receiver
-keep class com.safecallkids.app.CallReceiver { *; }

# Keep service
-keep class com.safecallkids.app.CallBlockingService { *; }

# Keep MainActivity for system interactions
-keep class com.safecallkids.app.MainActivity { *; }

# Preserve telecom and phone state classes
-keep class android.telecom.** { *; }
-dontwarn android.telecom.**

# Keep phone state and call handling related classes
-keep class android.telephony.** { *; }
-dontwarn android.telephony.**

# Preserve SharedPreferences access
-keepclassmembers class * {
    *** get*SharedPreferences(...);
}

# Keep Android annotations
-keep class androidx.annotation.** { *; }

# Preserve line numbers for debugging crashes in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
