# 2026 R8 "Gold Standard" Rules
# Focus: Surgical protection to maximize binary shrinking and startup speed.

# 1. Annotation-based Protection
-keep @androidx.annotation.Keep class * { *; }

# 2. Koin Constructor Preservation
# Scoped to app packages so R8 can still shrink third-party dependencies
-keepclassmembers class com.ogabassey.contactscleaner.** {
    public <init>(...);
}

# 3. WorkManager integration
# KoinWorkerFactory is instantiated by Koin for WorkManager
-keep class com.ogabassey.contactscleaner.di.KoinWorkerFactory { *; }

# 4. Room KMP (2026 Rules for BundledSQLiteDriver)
# Keep RoomDatabase subclasses AND their members (DAO accessors called by name in _Impl)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.paging.LimitOffsetPagingSource {*;}
-keep interface com.ogabassey.contactscleaner.data.db.dao.** { *; }
-keep @androidx.room.Entity class * { *; }

# 5. Kotlinx Serialization
# Keep Companion objects and serializer() methods for @Serializable classes
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*
-keepclassmembers class com.ogabassey.contactscleaner.** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.ogabassey.contactscleaner.**$$serializer { *; }
-keepclassmembers class com.ogabassey.contactscleaner.** {
    *** Companion;
}
-keepclasseswithmembers class com.ogabassey.contactscleaner.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 6. Compose Stability
-keep class androidx.compose.runtime.Recomposer { *; }
