# =================================================================--
# R8 / ProGuard Keep Rules for FileManager Pro
# =================================================================--

# ------------------------------------------------------------------
# SHIZUKU API & IPC BINDER KEEP RULES (Full Integrity & Reflection)
# ------------------------------------------------------------------
-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep interface moe.shizuku.** { *; }
-keep class rikka.shizuku.provider.** { *; }
-keep class rikka.shizuku.shared.** { *; }

# Keep Shizuku Manager & Engine Classes
-keep class com.shizuku.filemanager.shizuku.** { *; }
-keep class com.shizuku.filemanager.fs.engine.ShizukuFileEngine { *; }

# Preserve Binder IPC Interfaces & Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.IInterface {
    *** asInterface(...);
}

# Preserve Reflection into ProcessBuilder & System APIs
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions, SourceFile, LineNumberTable

# ------------------------------------------------------------------
# JETPACK COMPOSE & KOTLIN COROUTINES
# ------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------
# ROOM DATABASE & DAOS
# ------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep interface * {
    @androidx.room.Dao *;
}
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------
# OKHTTP & OKIO
# ------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ------------------------------------------------------------------
# MEDIA3 & EXOPLAYER
# ------------------------------------------------------------------
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# ------------------------------------------------------------------
# FFMPEG KIT
# ------------------------------------------------------------------
-keep class com.arthenica.ffmpegkit.** { *; }

# ------------------------------------------------------------------
# PDFBOX ANDROID
# ------------------------------------------------------------------
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# ------------------------------------------------------------------
# COIL IMAGE LOADER
# ------------------------------------------------------------------
-dontwarn coil.**
-keep class coil.** { *; }

# ------------------------------------------------------------------
# BIOMETRICS & CREDENTIALS
# ------------------------------------------------------------------
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**
