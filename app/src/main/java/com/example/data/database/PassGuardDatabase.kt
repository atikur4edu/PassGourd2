package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.VaultItemDao
import com.example.data.model.VaultItem

@Database(entities = [VaultItem::class], version = 1, exportSchema = false)
abstract class PassGuardDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao

    companion object {
        @Volatile
        private var INSTANCE: PassGuardDatabase? = null

        fun getInstance(context: Context): PassGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PassGuardDatabase::class.java,
                    "passguard_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
