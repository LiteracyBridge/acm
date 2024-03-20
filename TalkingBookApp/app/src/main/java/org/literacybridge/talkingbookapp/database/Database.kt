package org.literacybridge.talkingbookapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.literacybridge.talkingbookapp.App

@Database(entities = [ProgramContentEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programContentDao(): ProgramContentDao


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

