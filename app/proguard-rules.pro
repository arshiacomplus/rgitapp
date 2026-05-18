# ===============================================
# ! FIX: Ignore warnings for server-side Java classes
# ===============================================
-dontwarn java.lang.ProcessHandle
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.**
-dontwarn org.eclipse.jgit.**

# ===============================================
# * KEEP: Prevent R8 from stripping/obfuscating 
# reflection-heavy classes (Fixes NoSuchMethodException)
# ===============================================
-keep class org.eclipse.jgit.** { *; }
-keep class org.slf4j.** { *; }
-keep class org.slf4j.impl.** { *; }
-keep class com.jcraft.jsch.** { *; }
-keep class com.googlecode.javaewah.** { *; }

# NOTE: Keep OkHttp and Coroutines safe just in case
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class net.lingala.zip4j.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}