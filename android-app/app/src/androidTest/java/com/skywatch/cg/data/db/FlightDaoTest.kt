package com.skywatch.cg.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.siros.OperationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FlightDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FlightDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = database.flightDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    private fun createTestFlight(
        flightId: String = "TEST-001-2024-01-15",
        operationType: OperationType = OperationType.ARRIVAL,
        flightDate: LocalDate = LocalDate.of(2024, 1, 15),
        sirosRecordId: String = "TEST-001"
    ): FlightEntity {
        return FlightEntity(
            flightId = flightId,
            sirosRecordId = sirosRecordId,
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBGR",
            originName = "GUARULHOS",
            originTimezone = "America/Sao_Paulo",
            destination = "SBCG",
            destinationName = "CAMPO GRANDE",
            destinationTimezone = "America/Campo_Grande",
            operationType = operationType.name,
            scheduledDepartureUtc = "10:00",
            scheduledArrivalUtc = "11:30",
            flightDate = flightDate.toString(),
            arrivalDate = flightDate.toString(),
            validFrom = "2024-01-01",
            validTo = "2024-12-31",
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "EM OPERAÇÃO"
        )
    }

    @Test
    fun testInsertAndGetByFlightId() = runBlocking {
        val flight = createTestFlight()
        dao.insert(flight)

        val retrieved = dao.getByFlightId(flight.flightId)

        assertNotNull(retrieved)
        assertEquals(flight.flightId, retrieved?.flightId)
        assertEquals(flight.airline, retrieved?.airline)
        assertEquals(flight.flightNumber, retrieved?.flightNumber)
    }

    @Test
    fun testInsertMultipleAndQueryAll() = runBlocking {
        val arrival = createTestFlight(
            flightId = "TEST-001-2024-01-15",
            operationType = OperationType.ARRIVAL,
            sirosRecordId = "TEST-001"
        )
        val departure = createTestFlight(
            flightId = "TEST-002-2024-01-15",
            operationType = OperationType.DEPARTURE,
            sirosRecordId = "TEST-002"
        )

        dao.insertAll(listOf(arrival, departure))

        val allArrivals = dao.getAllArrivals()
        val allDepartures = dao.getAllDepartures()

        assertEquals(1, allArrivals.size)
        assertEquals(1, allDepartures.size)
        assertEquals("TEST-001-2024-01-15", allArrivals[0].flightId)
        assertEquals("TEST-002-2024-01-15", allDepartures[0].flightId)
    }

    @Test
    fun testGetFlightsByDate() = runBlocking {
        val date1 = LocalDate.of(2024, 1, 15)
        val date2 = LocalDate.of(2024, 1, 16)

        val arrival1 = createTestFlight(flightId = "TEST-001-2024-01-15", flightDate = date1, sirosRecordId = "TEST-001")
        val departure1 = createTestFlight(flightId = "TEST-002-2024-01-15", flightDate = date1, operationType = OperationType.DEPARTURE, sirosRecordId = "TEST-002")
        val arrival2 = createTestFlight(flightId = "TEST-003-2024-01-16", flightDate = date2, sirosRecordId = "TEST-003")

        dao.insertAll(listOf(arrival1, departure1, arrival2))

        val flights15 = dao.getFlightsByDate(date1.toString())
        val flights16 = dao.getFlightsByDate(date2.toString())

        assertEquals(2, flights15.size)
        assertEquals(1, flights16.size)
    }

    @Test
    fun testUpdateFlight() = runBlocking {
        val flight = createTestFlight()
        dao.insert(flight)

        val updated = flight.copy(airline = "LATAM")
        dao.update(updated)

        val retrieved = dao.getByFlightId(flight.flightId)
        assertEquals("LATAM", retrieved?.airline)
    }

    @Test
    fun testDeleteFlight() = runBlocking {
        val flight = createTestFlight()
        dao.insert(flight)

        dao.delete(flight)

        val retrieved = dao.getByFlightId(flight.flightId)
        assertNull(retrieved)
    }

    @Test
    fun testDeleteOldFlights() = runBlocking {
        val oldDate = LocalDate.of(2023, 12, 31)
        val newDate = LocalDate.of(2024, 1, 15)

        val oldFlight = createTestFlight(
            flightId = "OLD-2023-12-31",
            flightDate = oldDate,
            sirosRecordId = "OLD"
        )
        val newFlight = createTestFlight(
            flightId = "NEW-2024-01-15",
            flightDate = newDate,
            sirosRecordId = "NEW"
        )

        dao.insertAll(listOf(oldFlight, newFlight))

        val deletedCount = dao.deleteOldFlights(newDate.toString())

        assertEquals(1, deletedCount)
        assertNull(dao.getByFlightId("OLD-2023-12-31"))
        assertNotNull(dao.getByFlightId("NEW-2024-01-15"))
    }

    @Test
    fun testPreventDuplicatesByPrimaryKey() = runBlocking {
        val flight1 = createTestFlight()
        val flight2 = flight1.copy(airline = "AZUL")

        dao.insert(flight1)
        dao.insert(flight2)

        val allFlights = dao.getAllArrivals()
        assertEquals(1, allFlights.size)
        assertEquals("AZUL", allFlights[0].airline)
    }

    @Test
    fun testClearAll() = runBlocking {
        val flight1 = createTestFlight(flightId = "TEST-001-2024-01-15", sirosRecordId = "TEST-001")
        val flight2 = createTestFlight(flightId = "TEST-002-2024-01-15", sirosRecordId = "TEST-002")

        dao.insertAll(listOf(flight1, flight2))
        dao.deleteAll()

        assertEquals(0, dao.getAllArrivals().size)
        assertEquals(0, dao.getAllDepartures().size)
    }
}