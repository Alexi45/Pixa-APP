# El motor del juego es Kotlin puro sin reflexión, así que se puede ofuscar entero.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─────────────────────────────────────────────────────────────────────────────
# El SDK de anuncios arrastra WorkManager, que por dentro usa Room. Room crea
# por reflexión las clases *_Impl que genera al compilar (Class.forName), así
# que R8 en modo completo se las lleva por delante y la app revienta al
# arrancar con "Failed to create an instance of androidx.work.impl.WorkDatabase".
# Hay que conservarles el nombre y el constructor vacío.
# ─────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**
