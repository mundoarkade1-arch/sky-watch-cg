package com.skywatch.cg.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.support.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        androidx.room.testing.MigrationTestHelper.SQLiteDriverProvider()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2PreservesFlights() {
        // Create database in version 1 and insert a flight
        val db: SupportSQLiteDatabase = helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO flights (
                    flightId, sirosRecordId, airline, flightNumber, equipment,
                    origin, originName, originTimezone, destination, destinationName,
                    destinationTimezone, operationType, scheduledDepartureUtc, scheduledArrivalUtc,
                    flightDate, validFrom, validTo, serviceType, sirosStatus,
                    operationalStatus, delayMinutes, lastSyncAt, statusUpdatedAt
                ) VALUES (
                    'GLO-1234-2024-10-15', 'GLO-001', 'GOL', '1234', 'B738',
                    'SBGR', 'GUARULHOS', 'America/Sao_Paulo', 'SBCG', 'CAMPO GRANDE',
                    'America/Campo_Grande', 'ARRIVAL', '08:00', '09:30',
                    '2024-10-15', '2024-10-01', '2025-03-31', 'REGULAR', 'Em Operação',
                    'UNKNOWN', NULL, 1700000000000, NULL
                )
                """.trimIndent()
            )
            close()
        }

        // Open database with version 2 and apply MIGRATION_1_2
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        // Verify that the flight data is fully preserved
        val cursor = migratedDb.query("SELECT * FROM flights WHERE flightId = 'GLO-1234-2024-10-15'")
        assertNotNull(cursor)
        assertEquals(true, cursor.moveToFirst())
        
        val airlineIndex = cursor.getColumnIndex("airline")
        assertEquals("GOL", cursor.getString(airlineIndex))
        
        cursor.close()
    }
}
