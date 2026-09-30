# ---- Kotlin ----
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Room entities, DAOs, database ----
-keep class com.guddiz.app.data.local.entity.** { *; }
-keep class com.guddiz.app.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# ---- Kotlin metadata (needed for reflection-based libs) ----
-keep class kotlin.Metadata { *; }

# ---- Suppress warnings that aren't actionable ----
-dontwarn org.jetbrains.annotations.**
-dontwarn androidx.room.paging.**
-dontwarn coil.**
-dontwarn kotlinx.coroutines.debug.**
