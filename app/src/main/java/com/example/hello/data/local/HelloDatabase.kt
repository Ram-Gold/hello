package com.example.hello.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.hello.data.local.dao.CaptureFlowDao
import com.example.hello.data.local.dao.ContactDao
import com.example.hello.data.local.dao.EncounterDao
import com.example.hello.data.local.dao.FollowUpDao
import com.example.hello.data.local.dao.PlaceDao
import com.example.hello.data.local.dao.SyncMetadataDao
import com.example.hello.data.local.dao.TagDao
import com.example.hello.data.local.entity.CaptureFlowComponentEntity
import com.example.hello.data.local.entity.CaptureFlowPresetEntity
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import com.example.hello.data.local.entity.EncounterEntity
import com.example.hello.data.local.entity.FollowUpEntity
import com.example.hello.data.local.entity.PersonEncounterCrossRef
import com.example.hello.data.local.entity.PersonFollowUpCrossRef
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
        PersonEncounterCrossRef::class,
        PlaceEntity::class,
        TagEntity::class,
        FollowUpEntity::class,
        PersonFollowUpCrossRef::class,
        CaptureFlowPresetEntity::class,
        CaptureFlowComponentEntity::class
    ],
    version = 10,
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
    abstract fun captureFlowDao(): CaptureFlowDao

    companion object {
        const val DATABASE_NAME = "hello_crm.db"

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE contacts ADD COLUMN notionistConfig TEXT DEFAULT NULL"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `contacts_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `aliases` TEXT NOT NULL,
                        `role` TEXT,
                        `company` TEXT,
                        `birthday` TEXT,
                        `phone` TEXT,
                        `email` TEXT,
                        `tagIds` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `photoUri` TEXT,
                        `notionistConfig` TEXT,
                        `firstMetAt` TEXT,
                        `lastMetAt` TEXT,
                        `meetCount` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `updatedAt` TEXT,
                        `customProperties` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO `contacts_new` (
                        `id`, `name`, `aliases`, `role`, `company`, `birthday`,
                        `phone`, `email`, `tagIds`, `notes`, `photoUri`, `notionistConfig`,
                        `firstMetAt`, `lastMetAt`, `meetCount`, `isArchived`, `isFavorite`,
                        `updatedAt`, `customProperties`
                    )
                    SELECT
                        `id`, `name`, `aliases`, `role`, `company`, `birthday`,
                        `phone`, `email`, `tagIds`, `notes`, `photoUri`, `notionistConfig`,
                        `firstMetAt`, `lastMetAt`, `meetCount`, `isArchived`, `isFavorite`,
                        `updatedAt`, `customProperties`
                    FROM `contacts`
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE `contacts`")
                connection.execSQL("ALTER TABLE `contacts_new` RENAME TO `contacts`")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `follow_ups` ADD COLUMN `hasExplicitTime` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `person_encounters` (
                        `encounterId` TEXT NOT NULL,
                        `contactId` TEXT NOT NULL,
                        PRIMARY KEY(`encounterId`, `contactId`)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_person_encounters_contactId` ON `person_encounters` (`contactId`)"
                )
                connection.execSQL(
                    "INSERT OR IGNORE INTO `person_encounters` (`encounterId`, `contactId`) SELECT `id`, `contactId` FROM `encounters`"
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `person_follow_ups` (
                        `followUpId` TEXT NOT NULL,
                        `contactId` TEXT NOT NULL,
                        PRIMARY KEY(`followUpId`, `contactId`)
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_person_follow_ups_contactId` ON `person_follow_ups` (`contactId`)"
                )
                connection.execSQL(
                    "INSERT OR IGNORE INTO `person_follow_ups` (`followUpId`, `contactId`) SELECT `id`, `contactId` FROM `follow_ups`"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_contacts_isArchived` ON `contacts` (`isArchived`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_contacts_name` ON `contacts` (`name`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_contacts_lastMetAt` ON `contacts` (`lastMetAt`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_encounters_contactId` ON `encounters` (`contactId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_encounters_placeId` ON `encounters` (`placeId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_encounters_occurredAt` ON `encounters` (`occurredAt`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_follow_ups_contactId` ON `follow_ups` (`contactId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_follow_ups_doneAt` ON `follow_ups` (`doneAt`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_follow_ups_dueAt` ON `follow_ups` (`dueAt`)"
                )
            }
        }

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
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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
