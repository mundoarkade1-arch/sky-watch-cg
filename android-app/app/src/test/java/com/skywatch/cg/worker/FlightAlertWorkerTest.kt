package com.skywatch.cg.worker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.db.AlertSent
import com.skywatch.cg.data.db.FlightEntity
import com.skywatch.cg.data.siros.OperationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun alertWindowIncludesFifteenThroughThirtyMinutesOnly() {
        assertTrue(FlightAlertWorker.isWithinAlertWindow(15))
        assertTrue(FlightAlertWorker.isWithinAlertWindow(30))
        assertFalse(FlightAlertWorker.isWithinAlertWindow(14))
        assertFalse(FlightAlertWorker.isWithinAlertWindow(31))
        assertFalse(FlightAlertWorker.isWithinAlertWindow(0))
    }

    @Test
    fun disabledAlertsAreNotProcessedByLegacyWorker() {
        assertFalse(FlightAlertWorker.shouldProcessAlerts(false))
        assertTrue(FlightAlertWorker.shouldProcessAlerts(true))
    }

    @Test
    fun eventDateUsesArrivalDateForArrivalsAndFlightDateForDepartures() {
        val flightDate = LocalDate.of(2024, 10, 15)
        val arrivalDate = flightDate.plusDays(1)

        assertEquals(
            arrivalDate,
            FlightAlertWorker.eventUtcDate(flightDate, arrivalDate, OperationType.ARRIVAL)
        )
        assertEquals(
            flightDate,
            FlightAlertWorker.eventUtcDate(flightDate, arrivalDate, OperationType.DEPARTURE)
        )
    }

    @Test
    fun alertSentRecordCanOnlyBeInsertedOnce() = runBlocking {
        val alert = AlertSent(
            id = FlightAlertWorker.alertId(
                "G3-1234-2024-10-15",
                OperationType.DEPARTURE,
                LocalDate.of(2024, 10, 15)
            ),
            flightId = "G3-1234-2024-10-15",
            operationType = OperationType.DEPARTURE.name,
            eventDate = "2024-10-15",
            minutesBefore = 15,
            sentAt = 0L
        )

        assertNotEquals(-1L, db.alertSentDao().insertIfAbsent(alert))
        assertEquals(-1L, db.alertSentDao().insertIfAbsent(alert))
        assertTrue(db.alertSentDao().isAlertSent(alert.id))
    }
}
