package com.skywatch.cg.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationJvmTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-jvm-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration6To7RunsOnJvmAndPreservesCacheMetadataAndAlert() = runBlocking {
        createV6Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        try {
            val sqlite = database.openHelper.writableDatabase
            sqlite.query("PRAGMA table_info(`flights`)").use { cursor ->
                val columns = buildList {
                    while (cursor.moveToNext()) add(cursor.getString(1))
                }
                assertEquals(true, columns.contains("sirosStage"))
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

    private fun createV6Database() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `flights` (" +
                        "`flightId` TEXT NOT NULL, `sirosRecordId` TEXT NOT NULL, `airline` TEXT NOT NULL, " +
                        "`flightNumber` TEXT NOT NULL, `equipment` TEXT, `origin` TEXT NOT NULL, " +
                        "`originName` TEXT, `originTimezone` TEXT NOT NULL, `destination` TEXT NOT NULL, " +
                        "`destinationName` TEXT, `destinationTimezone` TEXT NOT NULL, `operationType` TEXT NOT NULL, " +
                        "`scheduledDepartureUtc` TEXT NOT NULL, `scheduledArrivalUtc` TEXT NOT NULL, " +
                        "`flightDate` TEXT NOT NULL, `validFrom` TEXT NOT NULL, `validTo` TEXT NOT NULL, " +
                        "`serviceType` TEXT, `sirosStatus` TEXT NOT NULL, `operationalStatus` TEXT NOT NULL, " +
                        "`delayMinutes` INTEGER, `lastSyncAt` INTEGER NOT NULL, `statusUpdatedAt` INTEGER, " +
                        "PRIMARY KEY(`flightId`))"
                )
                db.execSQL(
                    "INSERT INTO flights (flightId, sirosRecordId, airline, flightNumber, origin, originTimezone, " +
                        "destination, destinationTimezone, operationType, scheduledDepartureUtc, " +
                        "scheduledArrivalUtc, flightDate, validFrom, validTo, sirosStatus, operationalStatus, lastSyncAt) " +
                        "VALUES ('OLD-1', 'OLD', 'GOL', '1234', 'SBGR', 'America/Sao_Paulo', 'SBCG', " +
                        "'America/Campo_Grande', 'ARRIVAL', '10:00', '11:30', '2024-01-15', '2024-01-01', " +
                        "'2024-12-31', 'EM OPERACAO', 'UNKNOWN', 0)"
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
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
}
