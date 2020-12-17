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

-dontskipnonpubliclibraryclasses
-dontoptimize
-dontpreverify
-dontobfuscate
-verbose

-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepattributes *Annotation*

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,includedescriptorclasses public class * extends android.view.View {
    void set*(***);
    *** get*();
}

-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# material
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

# androidx
#-dontwarn androidx.core.**
-dontnote androidx.core.**
-dontnote androidx.fragment.app.FragmentTransition
-dontnote androidx.versionedparcelable.VersionedParcel
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# bitcoinj
-keep,includedescriptorclasses class org.bitcoinj.wallet.Protos$** { *; }
-keepclassmembers class org.bitcoinj.wallet.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-keep,includedescriptorclasses class org.bitcoin.protocols.payments.Protos$** { *; }
-keepclassmembers class org.bitcoin.protocols.payments.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-dontwarn org.bitcoinj.store.WindowsMMapHack
-dontwarn org.bitcoinj.store.LevelDBBlockStore
-dontnote org.bitcoinj.crypto.DRMWorkaround
-dontnote org.bitcoinj.crypto.TrustStoreLoader$DefaultTrustStoreLoader
-dontnote com.subgraph.orchid.crypto.PRNGFixes
-dontwarn okio.DeflaterSink
-dontwarn okio.Okio
-dontnote com.squareup.okhttp.internal.Platform
-dontwarn org.bitcoinj.store.LevelDBFullPrunedBlockStore**
-dontwarn org.bitcoinj.protocols.channels.PaymentChannelClient

# bouncycastle
-dontwarn javax.naming.**

# protobuf-java
-dontnote com.google.protobuf.Android
-dontnote com.google.protobuf.ByteBufferWriter
-dontnote com.google.protobuf.GeneratedMessageLite$SerializedForm
-dontnote com.google.protobuf.UnsafeUtil

# zxing
-dontwarn com.google.zxing.common.BitMatrix

# Guava
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
-dontwarn com.google.errorprone.annotations.**
-dontwarn afu.org.checkerframework.checker.**,org.checkerframework.checker.**
-dontnote com.google.common.reflect.**
-dontnote com.google.appengine.**
-dontnote com.google.apphosting.**
-dontnote com.google.common.hash.Striped64,com.google.common.hash.Striped64$Cell
-dontnote com.google.common.cache.Striped64,com.google.common.cache.Striped64$Cell
-dontnote com.google.common.util.concurrent.AbstractFuture$UnsafeAtomicHelper

# OkHttp
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontnote okhttp3.internal.platform.ConscryptPlatform
-dontnote okhttp3.internal.platform.AndroidPlatform,okhttp3.internal.platform.AndroidPlatform$CloseGuard
-dontnote okhttp3.internal.platform.Platform

# Moshi
-dontnote com.squareup.moshi.**

# slf4j
-dontwarn org.slf4j.MDC
-dontwarn org.slf4j.MarkerFactory

# logback-android
-dontwarn javax.mail.**
-dontwarn brut.androlib.res.decoder.AXmlResourceParser
-dontnote android.app.AppGlobals

# Lambdas
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

-dontwarn android.**

# glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}
# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

# butterknife
-keep public class * implements butterknife.Unbinder { public <init>(**, android.view.View); }
#-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
