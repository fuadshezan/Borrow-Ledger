package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.PaymentDao
import com.example.data.local.dao.PersonDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SyncMetadataEntity

@Database(
    entities = [
        PersonEntity::class,
        LoanEntity::class,
        PaymentEntity::class,
        ReminderEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun loanDao(): LoanDao
    abstract fun paymentDao(): PaymentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lending_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
