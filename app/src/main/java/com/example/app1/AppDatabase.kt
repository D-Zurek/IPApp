package com.example.app1.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Fingerprint::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fingerprintDao(): FingerprintDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fingerprint_db"
                ).build().also {INSTANCE = it}
                INSTANCE = instance
                instance
            }
        }
    }
}
