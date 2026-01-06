# ============= Flash Trade ProGuard Rules =============
# Generated: 2026-01-06

# ============= Crash Reporting =============
# Keep source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============= Data Models =============

# Retrofit/Moshi DTOs - prevent obfuscation
-keep class com.otistran.flash_trade.data.remote.dto.** { *; }
-keepclassmembers class com.otistran.flash_trade.data.remote.dto.** { *; }

# Domain models
-keep class com.otistran.flash_trade.domain.model.** { *; }

# Room entities
-keep class com.otistran.flash_trade.data.local.database.entity.** { *; }

# ============= Moshi =============
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}
-keep class **JsonAdapter {
    <init>(...);
}

# ============= Retrofit =============
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ============= OkHttp =============
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============= Web3j =============
-keep class org.web3j.** { *; }
-dontwarn org.web3j.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**

# ============= Privy SDK =============
-keep class io.privy.** { *; }
-keep interface io.privy.** { *; }
-dontwarn io.privy.**

# ============= Kotlinx Serialization =============
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.otistran.flash_trade.**$$serializer { *; }
-keepclassmembers class com.otistran.flash_trade.** {
    *** Companion;
}
-keepclasseswithmembers class com.otistran.flash_trade.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============= Compose =============
-dontwarn androidx.compose.**

# ============= Android Specific =============

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============= WorkManager =============
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ============= Hilt =============
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ============= Timber =============
-dontwarn timber.log.**

# ============= Security =============
# Repackage all classes to 'ft' package for smaller APK and obfuscation
-repackageclasses 'ft'
-allowaccessmodification

# Remove Timber logging calls in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}