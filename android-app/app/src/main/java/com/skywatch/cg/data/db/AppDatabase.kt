package com.skywatch.cg.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FlightEntity::class, SyncMetadata::class, FollowedFlight::class, AlertSent::class], version = 7, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun flightDao(): FlightDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun followedFlightDao(): FollowedFlightDao
    abstract fun alertSentDao(): AlertSentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "sky_watch_cg.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_metadata` (
                        `id` INTEGER NOT NULL,
                        `lastSuccessfulSyncAt` INTEGER NOT NULL,
                        `lastSyncStatus` TEXT NOT NULL,
                        `lastSyncErrorMessage` TEXT,
                        `recordsInserted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        CHECK (`id` = 1)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar coluna arrivalDate na tabela flights
                database.execSQL(
                    "ALTER TABLE `flights` ADD COLUMN `arrivalDate` TEXT NOT NULL DEFAULT ''"
                )
                // Atualizar arrivalDate existente = flightDate para voos já persistidos
                database.execSQL(
                    "UPDATE `flights` SET `arrivalDate` = `flightDate` WHERE `arrivalDate` = ''"
                )
                // Preservar a regra legada; a migração v5→v6 substitui CHECK por triggers uniformes.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_metadata_new` (
                        `id` INTEGER NOT NULL,
                        `lastSuccessfulSyncAt` INTEGER NOT NULL,
                        `lastSyncStatus` TEXT NOT NULL,
                        `lastSyncErrorMessage` TEXT,
                        `recordsInserted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        CHECK (`id` = 1)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO `sync_metadata_new` SELECT * FROM `sync_metadata`"
                )
                database.execSQL("DROP TABLE IF EXISTS `sync_metadata`")
                database.execSQL("ALTER TABLE `sync_metadata_new` RENAME TO `sync_metadata`")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tabela followed_flights para voos que o usuário quer seguir
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `followed_flights` (
                        `flightId` TEXT NOT NULL,
                        `airline` TEXT NOT NULL,
                        `flightNumber` TEXT NOT NULL,
                        `origin` TEXT NOT NULL,
                        `destination` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `scheduledDepartureUtc` INTEGER NOT NULL,
                        `scheduledArrivalUtc` INTEGER NOT NULL,
                        `flightDate` TEXT NOT NULL,
                        `arrivalDate` TEXT NOT NULL,
                        `originTimezone` TEXT NOT NULL,
                        `destinationTimezone` TEXT NOT NULL,
                        `originName` TEXT,
                        `destinationName` TEXT,
                        `equipment` TEXT,
                        `serviceType` TEXT,
                        `daysOfWeek` TEXT NOT NULL,
                        `validFrom` TEXT NOT NULL,
                        `validTo` TEXT NOT NULL,
                        `sirosStatus` TEXT,
                        `sirosRecordId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                        PRIMARY KEY(`flightId`)
                    )
                    """.trimIndent()
                )
                // Tabela alert_sent para deduplicação de notificações
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `alert_sent` (
                        `id` TEXT NOT NULL,
                        `flightId` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `eventDate` TEXT NOT NULL,
                        `minutesBefore` INTEGER NOT NULL,
                        `sentAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_alert_sent_flight_date` ON `alert_sent` (`flightId`, `eventDate`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_alert_sent_flight_date` ON `alert_sent` (`flightId`, `eventDate`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE `sync_metadata_new` (
                        `id` INTEGER NOT NULL,
                        `lastSuccessfulSyncAt` INTEGER NOT NULL,
                        `lastSyncStatus` TEXT NOT NULL,
                        `lastSyncErrorMessage` TEXT,
                        `recordsInserted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "INSERT INTO `sync_metadata_new` SELECT * FROM `sync_metadata`"
                )
                database.execSQL("DROP TABLE `sync_metadata`")
                database.execSQL("ALTER TABLE `sync_metadata_new` RENAME TO `sync_metadata`")
                createSyncMetadataIdTriggers(database)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `flights` RENAME TO `flights_old`")
                database.execSQL(
                    """
                    CREATE TABLE `flights` (
                        `flightId` TEXT NOT NULL,
                        `sirosRecordId` TEXT NOT NULL,
                        `sirosStage` TEXT NOT NULL,
                        `airline` TEXT NOT NULL,
                        `flightNumber` TEXT NOT NULL,
                        `equipment` TEXT,
                        `origin` TEXT NOT NULL,
                        `originName` TEXT,
                        `originTimezone` TEXT NOT NULL,
                        `destination` TEXT NOT NULL,
                        `destinationName` TEXT,
                        `destinationTimezone` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `scheduledDepartureUtc` TEXT NOT NULL,
                        `scheduledArrivalUtc` TEXT NOT NULL,
                        `flightDate` TEXT NOT NULL,
                        `arrivalDate` TEXT NOT NULL,
                        `validFrom` TEXT NOT NULL,
                        `validTo` TEXT NOT NULL,
                        `serviceType` TEXT,
                        `sirosStatus` TEXT NOT NULL,
                        `operationalStatus` TEXT NOT NULL,
                        `delayMinutes` INTEGER,
                        `lastSyncAt` INTEGER NOT NULL,
                        `statusUpdatedAt` INTEGER,
                        PRIMARY KEY(`flightId`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `flights` (
                        `flightId`, `sirosRecordId`, `sirosStage`, `airline`, `flightNumber`, `equipment`,
                        `origin`, `originName`, `originTimezone`, `destination`, `destinationName`,
                        `destinationTimezone`, `operationType`, `scheduledDepartureUtc`, `scheduledArrivalUtc`,
                        `flightDate`, `arrivalDate`, `validFrom`, `validTo`, `serviceType`, `sirosStatus`,
                        `operationalStatus`, `delayMinutes`, `lastSyncAt`, `statusUpdatedAt`
                    )
                    SELECT `flightId`, `sirosRecordId`, '', `airline`, `flightNumber`, `equipment`,
                        `origin`, `originName`, `originTimezone`, `destination`, `destinationName`,
                        `destinationTimezone`, `operationType`, `scheduledDepartureUtc`, `scheduledArrivalUtc`,
                        `flightDate`, `arrivalDate`, `validFrom`, `validTo`, `serviceType`, `sirosStatus`,
                        `operationalStatus`, `delayMinutes`, `lastSyncAt`, `statusUpdatedAt`
                    FROM `flights_old`
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE `flights_old`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .addCallback(SYNC_METADATA_ID_CALLBACK)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        internal val SYNC_METADATA_ID_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createSyncMetadataIdTriggers(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                createSyncMetadataIdTriggers(db)
            }
        }

        private fun createSyncMetadataIdTriggers(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `trg_sync_metadata_id_insert`
                BEFORE INSERT ON `sync_metadata`
                WHEN NEW.`id` != 1
                BEGIN
                    SELECT RAISE(ABORT, 'sync_metadata.id must be 1');
                END
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `trg_sync_metadata_id_update`
                BEFORE UPDATE OF `id` ON `sync_metadata`
                WHEN NEW.`id` != 1
                BEGIN
                    SELECT RAISE(ABORT, 'sync_metadata.id must be 1');
                END
                """.trimIndent()
            )
        }
    }
}
