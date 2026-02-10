# 2026 R8 "Gold Standard" Rules
# Focus: Surgical protection to maximize binary shrinking and startup speed.

# 1. Annotation-based Protection (2026 Best Practice)
-keep @androidx.annotation.Keep class * { *; }
-keep @org.koin.core.annotation.KoinInternalApi class * { *; }
-keep @org.koin.core.annotation.KoinApi class * { *; }

# 2. Koin Constructor Preservation
# Ensure Koin can instantiate ViewModels and Repositories via reflection/factory
-keepclassmembers class * {
    public <init>(...);
}

# 3. KMP Infrastructure
# Protect the bridge between Android and Common code
-keep class com.ogabassey.contactscleaner.platform.Logger { *; }
-keep class com.ogabassey.contactscleaner.di.** { *; }

# 4. Room KMP (2026 Rules for BundledSQLiteDriver)
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.paging.LimitOffsetPagingSource {*;}
-keep interface com.ogabassey.contactscleaner.data.db.dao.** { *; }
-keep @androidx.room.Entity class * { *; }

# 5. Kotlinx Serialization (Surgical)
# Only keep fields with @SerialName to allow stripping of other metadata
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# 6. RevenueCat Core
-keep class com.revenuecat.purchases.** { *; }

# 7. Compose Stability
-keep class androidx.compose.runtime.Recomposer { *; }

