# Add project specific ProGuard rules here.

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- SQLCipher ---
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.**

# --- Strip verbose logs in release builds ---
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
