# Hello release R8 / ProGuard rules

# Preserve line numbers and source file names for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata & coroutines
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room database, DAOs, entities, and KSP-generated *_Impl classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.example.hello.data.local.**_Impl { *; }
-keepclassmembers class com.example.hello.data.local.** {
    @androidx.room.TypeConverter *;
}

# WorkManager workers instantiated via reflection
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.hello.platform.notification.CheckInDigestWorker { *; }
-keep class com.example.hello.platform.portability.ExportDataWorker { *; }

# Glance AppWidget & BroadcastReceivers
-keep class androidx.glance.** { *; }
-keep class com.example.hello.platform.widget.** { *; }
-keep class com.example.hello.platform.notification.*Receiver { *; }

# Application & Activities declared in AndroidManifest.xml
-keep class com.example.hello.HelloApplication { *; }
-keep class com.example.hello.MainActivity { *; }
-keep class com.example.hello.ui.quickaction.QuickCaptureActivity { *; }
