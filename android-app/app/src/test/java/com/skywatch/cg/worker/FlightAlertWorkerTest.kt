package com.skywatch.cg.worker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.db.FlightEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FlightAlertWorkerTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFlightEntityInsertionAndQuery() = runBlocking {
        val flight = FlightEntity(
            flightId = "G31234_2024-10-15_DEP",
            sirosRecordId = "rec1",
            airline = "G3",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBCG",
            originName = "Campo Grande",
            originTimezone = "America/Campo_Grande",
            destination = "SBGR",
            destinationName = "Guarulhos",
            destinationTimezone = "America/Sao_Paulo",
            operationType = "DEPARTURE",
            scheduledDepartureUtc = "10:00",
            scheduledArrivalUtc = "12:00",
            flightDate = "2024-10-15",
            arrivalDate = "2024-10-15",
            validFrom = "2024-10-01",
            validTo = "2024-10-31",
            serviceType = "N",
            sirosStatus = "CONFIRMADO"
        )

        db.flightDao().insert(flight)
        val departures = db.flightDao().getDeparturesByDate("2024-10-15")
        assertEquals(1, departures.size)
        assertEquals("1234", departures[0].flightNumber)
    }
}
