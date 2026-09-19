# R8 rules for the release build. The defaults from proguard-android-optimize.txt
# plus the consumer rules shipped by AndroidX cover almost everything here: the app
# has no reflection, no reflective serialization and no JNI.

# Keep line numbers so Play Console and the mapping file produce readable stack
# traces, while still hiding the original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room looks up the generated <Database>_Impl class by name at runtime.
# room-runtime ships this rule as a consumer rule; keeping it here means the app
# does not silently break if that ever changes.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
