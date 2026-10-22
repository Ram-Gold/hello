package com.example.hello.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.hello.data.local.dao.SyncMetadataDao
import com.example.hello.data.local.entity.SyncMetadataEntity

/**
 * Main Room database for "Hello".
 * Per AGENTS.md, Room acts as a disposable, rebuildable query cache while Markdown is truth.
 */
@Database(
    entities = [SyncMetadataEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HelloDatabase : RoomDatabase() {

    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        const val DATABASE_NAME = "hello_crm.db"

        @Volatile
        private var instance: HelloDatabase? = null

        fun getInstance(context: Context): HelloDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): HelloDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                HelloDatabase::class.java,
                DATABASE_NAME
            )
            .setDriver(BundledSQLiteDriver())
            .build()
        }

        fun createInMemory(context: Context): HelloDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext ?: context,
                HelloDatabase::class.java
            )
            .setDriver(BundledSQLiteDriver())
            .allowMainThreadQueries()
            .build()
        }
    }
}
