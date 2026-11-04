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
import java.time.LocalDate

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
            airline = "G3",
            flightNumber = "1234",
            origin = "SBCG",
            destination = "SBGR",
            operationType = "DEPARTURE",
            scheduledDepartureUtc = 36000L,
            scheduledArrivalUtc = 40000L,
            flightDate = "2024-10-15",
            arrivalDate = "2024-10-15",
            originTimezone = "America/Campo_Grande",
            destinationTimezone = "America/Sao_Paulo",
            originName = "Campo Grande",
            destinationName = "Guarulhos",
            equipment = "B738",
            serviceType = "N",
            daysOfWeek = "1234567",
            validFrom = "2024-10-01",
            validTo = "2024-10-31",
            sirosStatus = "CONFIRMADO",
            sirosRecordId = "rec1",
            createdAt = System.currentTimeMillis()
        )

        db.flightDao().insertFlights(listOf(flight))
        val departures = db.flightDao().getDeparturesByDate("2024-10-15")
        assertEquals(1, departures.size)
        assertEquals("1234", departures[0].flightNumber)
    }
}
