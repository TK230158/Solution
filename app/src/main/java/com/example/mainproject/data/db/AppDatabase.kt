package com.example.mainproject.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mainproject.data.db.dao.RegistrationDao
import com.example.mainproject.data.db.entity.PendingRegistration
import com.example.mainproject.data.db.entity.PendingRegistrationItem

@Database(
    entities = [
        PendingRegistration::class,
        PendingRegistrationItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun registrationDao(): RegistrationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crossvisionf_db"
                ).build().also { instance = it }
            }
        }
    }
}
