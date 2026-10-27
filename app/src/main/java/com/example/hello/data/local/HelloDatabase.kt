package com.example.hello.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.hello.data.local.dao.ContactDao
import com.example.hello.data.local.dao.EncounterDao
import com.example.hello.data.local.dao.FollowUpDao
import com.example.hello.data.local.dao.PlaceDao
import com.example.hello.data.local.dao.SyncMetadataDao
import com.example.hello.data.local.dao.TagDao
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import com.example.hello.data.local.entity.EncounterEntity
import com.example.hello.data.local.entity.FollowUpEntity
import com.example.hello.data.local.entity.PlaceEntity
import com.example.hello.data.local.entity.SyncMetadataEntity
import com.example.hello.data.local.entity.TagEntity

/**
 * Main Room database for "Hello".
 * Per AGENTS.md, Room acts as a disposable, rebuildable query cache while Markdown is truth.
 */
@Database(
    entities = [
        SyncMetadataEntity::class,
        ContactEntity::class,
        ContactFtsEntity::class,
        EncounterEntity::class,
        PlaceEntity::class,
        TagEntity::class,
        FollowUpEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HelloDatabase : RoomDatabase() {

    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun contactDao(): ContactDao
    abstract fun encounterDao(): EncounterDao
    abstract fun placeDao(): PlaceDao
    abstract fun tagDao(): TagDao
    abstract fun followUpDao(): FollowUpDao

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
            .fallbackToDestructiveMigration(true)
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
