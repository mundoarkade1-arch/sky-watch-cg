package com.skywatch.cg.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

// Valida MIGRATION_1_2 sobre uma "v1 provavel": tabela flights igual a gerada pelo Room hoje.
// O schema real da v1 nunca foi exportado, entao isto NAO prova migracao de aparelho real com a v1 antiga.
class AppDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dbName = "migration-test.db"

    private val createFlightsV1 =
        "CREATE TABLE IF NOT EXISTS `flights` (`flightId` TEXT NOT NULL, `sirosRecordId` TEXT NOT NULL, `airline` TEXT NOT NULL, `flightNumber` TEXT NOT NULL, `equipment` TEXT, `origin` TEXT NOT NULL, `originName` TEXT, `originTimezone` TEXT NOT NULL, `destination` TEXT NOT NULL, `destinationName` TEXT, `destinationTimezone` TEXT NOT NULL, `operationType` TEXT NOT NULL, `scheduledDepartureUtc` TEXT NOT NULL, `scheduledArrivalUtc` TEXT NOT NULL, `flightDate` TEXT NOT NULL, `validFrom` TEXT NOT NULL, `validTo` TEXT NOT NULL, `serviceType` TEXT, `sirosStatus` TEXT NOT NULL, `operationalStatus` TEXT NOT NULL, `delayMinutes` INTEGER, `lastSyncAt` INTEGER NOT NULL, `statusUpdatedAt` INTEGER, PRIMARY KEY(`flightId`))"

    private val insertFlightV1 =
        "INSERT INTO flights (flightId, sirosRecordId, airline, flightNumber, origin, originTimezone, destination, destinationTimezone, operationType, scheduledDepartureUtc, scheduledArrivalUtc, flightDate, validFrom, validTo, sirosStatus, operationalStatus, lastSyncAt) VALUES ('OLD-1', 'OLD', 'GOL', '1234', 'SBGR', 'America/Sao_Paulo', 'SBCG', 'America/Campo_Grande', 'ARRIVAL', '10:00', '11:30', '2024-01-15', '2024-01-01', '2024-12-31', 'EM OPERACAO', 'UNKNOWN', 0)"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV1Database() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(createFlightsV1)
                db.execSQL(insertFlightV1)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase
        helper.close()
    }

    private fun openMigrated(): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .addCallback(AppDatabase.SYNC_METADATA_ID_CALLBACK)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun migration1To7CreatesSyncMetadataAndKeepsFlights() = runBlocking {
        createV1Database()
        val database = openMigrated()
        try {
            val sqlite = database.openHelper.writableDatabase
            sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name='sync_metadata'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            assertNotNull(database.flightDao().getByFlightId("OLD-1"))
            sqlite.query("PRAGMA table_info(`flights`)").use { cursor ->
                var hasStage = false
                while (cursor.moveToNext()) {
                    hasStage = hasStage || cursor.getString(1) == "sirosStage"
                }
                assertEquals(true, hasStage)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migration4To5AddsAlertIndexAndKeepsSentRecords() {
        val initialDatabase = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        initialDatabase.openHelper.writableDatabase.execSQL(
            "INSERT INTO alert_sent (id, flightId, operationType, eventDate, minutesBefore, sentAt) " +
                "VALUES ('alert-1', 'flight-1', 'ARRIVAL', '2024-01-15', 15, 0)"
        )
        initialDatabase.close()

        context.openOrCreateDatabase(dbName, 0, null).use { database ->
            database.execSQL("DROP INDEX IF EXISTS `idx_alert_sent_flight_date`")
            database.version = 4
        }

        val database = openMigrated()
        try {
            val sqlite = database.openHelper.writableDatabase
            sqlite.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_alert_sent_flight_date'"
            ).use { cursor ->
                assertEquals(1, cursor.count)
            }
            sqlite.query("SELECT id FROM alert_sent WHERE id = 'alert-1'").use { cursor ->
                assertEquals(1, cursor.count)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migratedSyncMetadataRejectsIdDifferentFromOne() {
        createV1Database()
        val database = openMigrated()
        try {
            val sqlite = database.openHelper.writableDatabase
            try {
                sqlite.execSQL("INSERT INTO sync_metadata (id, lastSuccessfulSyncAt, lastSyncStatus, recordsInserted) VALUES (2, 0, 'OK', 0)")
                fail("A regra id = 1 deveria rejeitar id = 2")
            } catch (expected: SQLiteConstraintException) {
                // esperado
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun freshSyncMetadataRejectsInvalidIdOnInsertAndUpdate() = runBlocking {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addCallback(AppDatabase.SYNC_METADATA_ID_CALLBACK)
            .allowMainThreadQueries()
            .build()
        try {
            val sqlite = database.openHelper.writableDatabase
            assertInvalidSyncMetadataId(
                sqlite,
                "INSERT INTO sync_metadata " +
                    "(id, lastSuccessfulSyncAt, lastSyncStatus, recordsInserted) " +
                    "VALUES (2, 0, 'IDLE', 0)"
            )
            database.syncMetadataDao().upsertSyncMetadata(SyncMetadata(id = 1))
            assertInvalidSyncMetadataId(sqlite, "UPDATE sync_metadata SET id = 2 WHERE id = 1")
        } finally {
            database.close()
        }
    }

    @Test
    fun migration5To6RemovesCheckConstraintAndPreservesMetadata() = runBlocking {
        val initialDatabase = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        initialDatabase.openHelper.writableDatabase.execSQL(
            "INSERT INTO sync_metadata (id, lastSuccessfulSyncAt, lastSyncStatus, recordsInserted) " +
                "VALUES (1, 123, 'SUCCESS', 7)"
        )
        initialDatabase.close()

        context.openOrCreateDatabase(dbName, 0, null).use { database ->
            database.execSQL("DROP TRIGGER IF EXISTS `trg_sync_metadata_id_insert`")
            database.execSQL("DROP TRIGGER IF EXISTS `trg_sync_metadata_id_update`")
            database.execSQL("ALTER TABLE `sync_metadata` RENAME TO `sync_metadata_old`")
            database.execSQL(
                """
                CREATE TABLE `sync_metadata` (
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
            database.execSQL("INSERT INTO `sync_metadata` SELECT * FROM `sync_metadata_old`")
            database.execSQL("DROP TABLE `sync_metadata_old`")
            database.version = 5
        }

        val database = openMigrated()
        try {
            val sqlite = database.openHelper.writableDatabase
            val metadata = database.syncMetadataDao().getSyncMetadata()
            assertNotNull(metadata)
            assertEquals(123L, metadata?.lastSuccessfulSyncAt)
            assertEquals(7, metadata?.recordsInserted)
            sqlite.query(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='sync_metadata'"
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(false, cursor.getString(0).contains("CHECK"))
            }
            assertInvalidSyncMetadataId(
                sqlite,
                "INSERT INTO sync_metadata " +
                    "(id, lastSuccessfulSyncAt, lastSyncStatus, recordsInserted) " +
                    "VALUES (2, 0, 'IDLE', 0)"
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun migration6To7AddsStageAndPreservesMetadataAndAlerts() = runBlocking {
        createV6DatabaseWithRecords()
        val database = openMigrated()
        try {
            val sqlite = database.openHelper.writableDatabase
            sqlite.query("PRAGMA table_info(`flights`)").use { cursor ->
                var hasStage = false
                while (cursor.moveToNext()) {
                    hasStage = hasStage || cursor.getString(1) == "sirosStage"
                }
                assertEquals(true, hasStage)
            }
            assertNotNull(database.flightDao().getByFlightId("OLD-1"))
            assertNotNull(database.syncMetadataDao().getSyncMetadata())
            sqlite.query("SELECT id FROM alert_sent WHERE id = 'alert-v6'").use { cursor ->
                assertEquals(1, cursor.count)
            }
        } finally {
            database.close()
        }
    }

    private fun createV6DatabaseWithRecords() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(createFlightsV1)
                db.execSQL(insertFlightV1)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqlite = helper.writableDatabase
        AppDatabase.MIGRATION_1_2.migrate(sqlite)
        AppDatabase.MIGRATION_2_3.migrate(sqlite)
        AppDatabase.MIGRATION_3_4.migrate(sqlite)
        AppDatabase.MIGRATION_4_5.migrate(sqlite)
        AppDatabase.MIGRATION_5_6.migrate(sqlite)
        sqlite.execSQL(
            "INSERT INTO sync_metadata (id, lastSuccessfulSyncAt, lastSyncStatus, recordsInserted) " +
                "VALUES (1, 456, 'SUCCESS', 1)"
        )
        sqlite.execSQL(
            "INSERT INTO alert_sent (id, flightId, operationType, eventDate, minutesBefore, sentAt) " +
                "VALUES ('alert-v6', 'OLD-1', 'ARRIVAL', '2024-01-15', 15, 0)"
        )
        sqlite.version = 6
        helper.close()
    }

    private fun assertInvalidSyncMetadataId(database: SupportSQLiteDatabase, sql: String) {
        try {
            database.execSQL(sql)
            fail("A tabela sync_metadata deveria rejeitar id diferente de 1")
        } catch (expected: SQLiteConstraintException) {
            // esperado
        }
    }
}
