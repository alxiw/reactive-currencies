# Simple and reliable configuration file for R8/ProGuard in release builds.
# The modern R8 compiler automatically fetches proguard rules from bundled libraries (Retrofit, Room, DataStore).

# 1. Preserve metadata for readable crash reports (e.g., Firebase Crashlytics)
# Retain line numbers, generic signatures, and annotations
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Mask original source file names in stack traces with the word "SourceFile"
-renamesourcefileattribute SourceFile

# 2. Protect XML/JSON serialization models from obfuscation
# Since these classes are parsed via reflection (Retrofit XML converter / Room),
# their names and fields MUST NOT be renamed or stripped.

# Remote API Response Models (Simple XML framework)
-keep class io.github.alxiw.reactivecurrencies.data.** { *; }

# Simple XML Framework
-dontwarn org.simpleframework.xml.**
-keep class org.simpleframework.xml.** { *; }
-keep class * { @org.simpleframework.xml.** *; }

-keep class io.github.alxiw.reactivecurrencies.data.remote.model.** { *; }
-keep class io.github.alxiw.reactivecurrencies.data.local.model.** { *; }
