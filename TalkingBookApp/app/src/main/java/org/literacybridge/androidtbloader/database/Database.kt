package org.literacybridge.androidtbloader.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.literacybridge.androidtbloader.App

@Database(entities = [ProgramContentEntity::class, S3SyncEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programContentDao(): ProgramContentDao
    abstract fun s3SyncDoa(): S3SyncEntityDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    App.context,
                    AppDatabase::class.java,
                    "tbloader_db"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

}

