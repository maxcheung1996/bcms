# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ================================
# ESSENTIAL RULES FOR BCMS PROJECT
# ================================

# Preserve source file and line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep attributes needed for annotations and reflection
-keepattributes Signature
-keepattributes Annotation
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# ================================
# GSON / JSON SERIALIZATION RULES
# ================================

# Keep Gson classes and their methods
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep classes with @SerializedName annotation
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all DTO classes from data package
-keep class com.socam.bcms.data.dto.** { *; }
-keepclassmembers class com.socam.bcms.data.dto.** {
    <fields>;
    <methods>;
}

# Keep data classes for API responses
-keep class com.socam.bcms.data.model.** { *; }
-keepclassmembers class com.socam.bcms.data.model.** {
    <fields>;
    <methods>;
}

# Generic Gson rules for any data classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic signature of any class used by Gson
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================
# RETROFIT API RULES
# ================================

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-keepclassmembers class retrofit2.** {
    <methods>;
}
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep API service interfaces
-keep interface com.socam.bcms.data.api.** { *; }
-keepclassmembers interface com.socam.bcms.data.api.** {
    <methods>;
}

# Keep API service classes and methods
-keep class com.socam.bcms.data.api.** { *; }
-keepclassmembers class com.socam.bcms.data.api.** {
    <fields>;
    <methods>;
}

# ================================
# OKHTTP RULES
# ================================

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ================================
# SQLDELIGHT DATABASE RULES
# ================================

# Keep all SqlDelight generated classes
-keep class com.socam.bcms.database.** { *; }
-keepclassmembers class com.socam.bcms.database.** {
    <fields>;
    <methods>;
}

# Keep database queries and implementations
-keep class app.cash.sqldelight.** { *; }
-keep class com.squareup.sqldelight.** { *; }

# ================================
# KOTLIN SPECIFIC RULES
# ================================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep data classes
-keep class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class * {
    *** copy(...);
    *** component*();
}

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ================================
# MMKV PREFERENCES RULES
# ================================

# Keep MMKV classes (for encrypted preferences)
-keep class com.tencent.mmkv.** { *; }
-dontwarn com.tencent.mmkv.**

# ================================
# UHF VENDOR LIBRARY RULES
# ================================

# Keep all UHF related classes (vendor libraries)
-keep class com.uhf.** { *; }
-keep class com.iscan.** { *; }
-keepclassmembers class com.uhf.** {
    <fields>;
    <methods>;
}
-keepclassmembers class com.iscan.** {
    <fields>;
    <methods>;
}

# JNI related rules for UHF libraries
-keepclasseswithmembernames class * {
    native <methods>;
}

# ================================
# ANDROIDX & MATERIAL RULES
# ================================

# Keep ViewBinding classes
-keep class * implements androidx.viewbinding.ViewBinding { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ================================
# APPLICATION SPECIFIC RULES
# ================================

# Keep main application components
-keep class com.socam.bcms.BCMSApp { *; }
-keep class com.socam.bcms.MainActivity { *; }
-keep class com.socam.bcms.presentation.AuthActivity { *; }

# Keep ViewModels and their factories
-keep class com.socam.bcms.presentation.**.*ViewModel { *; }
-keep class com.socam.bcms.presentation.**.*ViewModelFactory { *; }

# Keep repository classes
-keep class com.socam.bcms.data.repository.** { *; }

# Keep configuration classes
-keep class com.socam.bcms.config.** { *; }

# ================================
# GENERAL ANDROID RULES
# ================================

# Keep custom views and their attributes
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    *** get*();
}

# Keep Fragment constructors
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# Keep parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ================================
# DEBUGGING RULES (Optional)
# ================================

# Keep class names for crash reports
-keepnames class * { *; }

# Suppress warnings for common libraries
-dontwarn java.lang.invoke.**
-dontwarn **$$serializer
-dontwarn javax.annotation.**
-dontwarn javax.inject.**