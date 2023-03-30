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

# Native methods
# https://www.guardsquare.com/en/products/proguard/manual/examples#native
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# App
-keep class tech.nagual.phoenix.** implements androidx.appcompat.view.CollapsibleActionView { *; }
-keep class me.zhanghai.android.files.provider.common.ByteString { *; }
-keep class me.zhanghai.android.files.provider.linux.syscall.** { *; }
-keepnames class * extends java.lang.Exception
# For Class.getEnumConstants()
-keepclassmembers enum * {
  public static **[] values();
}
-keepnames class tech.nagual.phoenix.** implements android.os.Parcelable

# Apache Commons Compress
-dontwarn org.apache.commons.compress.compressors.**
-dontwarn org.apache.commons.compress.archivers.**
# me.zhanghai.android.files.provider.archive.archiver.ArchiveWriter.sTarArchiveEntryLinkFlagsField
-keepclassmembers class org.apache.commons.compress.archivers.tar.TarArchiveEntry {
    byte linkFlag;
}

# Apache FtpServer
-keepclassmembers class * implements org.apache.mina.core.service.IoProcessor {
    public <init>(java.util.concurrent.ExecutorService);
    public <init>(java.util.concurrent.Executor);
    public <init>();
}

# Bouncy Castle
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# Stetho No-op
# This library includes the no-op for stetho-okhttp3 which requires okhttp3, but we never used it.
-dontwarn com.facebook.stetho.okhttp3.StethoInterceptor



-keep class tech.nagual.phoenix.tools.organizer.data.model.** { *; }

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class tech.nagual.phoenix.tools.organizer.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class tech.nagual.phoenix.tools.organizer.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class tech.nagual.phoenix.tools.organizer.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}

# librootjava
-keepclassmembers class * {
    public static void main(java.lang.String[]);
}
-keep class * implements android.os.IInterface {
    *;
}

# permissions
-keepattributes *Annotation*
-keepclassmembers class ** {
    @me.zhanghai.android.effortlesspermissions.AfterPermissionDenied <methods>;
}



##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

##---------------End: proguard configuration for Gson  ----------








-keep,includedescriptorclasses class com.mendhak.gpslogger.** { *; }

# --------------------------------------------------------------------------------- #
# GreenRobot EventBus
# https://github.com/greenrobot/EventBus/blob/master/HOWTO.md#proguard-configuration
# --------------------------------------------------------------------------------- #

-keepclassmembers,includedescriptorclasses class ** {
    public void onEvent*(***);
}

# Only required if you use AsyncExecutor
-keepclassmembers class * extends de.greenrobot.event.util.ThrowableFailureEvent {
    public <init>(java.lang.Throwable);
}

-keep,includedescriptorclasses class de.greenrobot.event.util.ErrorDialogManager.** { *; }

# Don't warn for missing support classes
-dontwarn de.greenrobot.event.util.*$Support
-dontwarn de.greenrobot.event.util.*$SupportManagerFragment


# --------------------------------------------------------------------------------- #
# Job Queue
# --------------------------------------------------------------------------------- #
-keep,includedescriptorclasses class com.path.android.** {*;}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}


# --------------------------------------------------------------------------------- #
# Action button
# --------------------------------------------------------------------------------- #
-keep,includedescriptorclasses class com.dd.processbutton.iml.ActionProcessButton { *; }


-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable

-dontwarn com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
-dontwarn com.bumptech.glide.load.resource.bitmap.Downsampler
-dontwarn com.bumptech.glide.load.resource.bitmap.HardwareConfigState
-dontwarn com.bumptech.glide.manager.RequestManagerRetriever

-keep public class * extends java.lang.Exception

# Joda
-dontwarn org.joda.convert.**
-dontwarn org.joda.time.**
-keep class org.joda.time.** { *; }
-keep interface org.joda.time.** { *; }

-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}


-keep class tech.nagual.common.** { *; }
-keep class tech.nagual.phoenix.tools.voicerecorder.** { *; }
-keep class tech.nagual.phoenix.tools.flashlight.** { *; }
-keep class me.zhanghai.android.files.features.gallery.** { *; }
-dontwarn android.graphics.Canvas
-dontwarn tech.nagual.**
-dontwarn tech.nagual.phoenix.tools.voicerecorder.**
-dontwarn tech.nagual.phoenix.tools.flashlight.**
-dontwarn me.zhanghai.android.files.features.gallery.**
-dontwarn org.apache.**

# Picasso
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# RenderScript
-keepclasseswithmembernames class * {
native <methods>;
}
-keep class androidx.renderscript.** { *; }

# Reprint
-keep class com.github.ajalt.reprint.module.** { *; }

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep TAG field, so it can be found by the SimpleDialog.show() method
-keepclassmembers class * extends tech.nagual.common.ui.dialogs.SimpleDialog {
    public static final java.lang.String TAG;
}