# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# JetPack Compose
-keep class androidx.compose.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep data classes used in Compose
-keep class com.ogabassey.contactscleaner.domain.model.** { *; }

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.paging.LimitOffsetPagingSource {*;}

