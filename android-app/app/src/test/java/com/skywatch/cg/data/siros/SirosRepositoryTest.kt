package com.skywatch.cg.data.siros

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SirosRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SirosRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SirosRepository(db, Dispatchers.IO, SirosRemoteSource.INSTANCE)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSaveAndRetrieveFlightsByDate() = runBlocking {
        val flightDate1 = LocalDate.of(2024, 10, 15)
        val flightDate2 = LocalDate.of(2024, 10, 16)

        val flight1 = NormalizedFlight(
            flightId = "GLO-1234-2024-10-15",
            sirosRecordId = "GLO-001",
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBCG",
            originName = "CAMPO GRANDE",
            originTimezone = "America/Campo_Grande",
            destination = "SBGR",
            destinationName = "GUARULHOS",
            destinationTimezone = "America/Sao_Paulo",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(10, 0),
            scheduledArrivalUtc = LocalTime.of(11, 30),
            flightDate = flightDate1,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )

        val flight2 = NormalizedFlight(
            flightId = "GLO-1234-2024-10-16",
            sirosRecordId = "GLO-001",
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBCG",
            originName = "CAMPO GRANDE",
            originTimezone = "America/Campo_Grande",
            destination = "SBGR",
            destinationName = "GUARULHOS",
            destinationTimezone = "America/Sao_Paulo",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(10, 0),
            scheduledArrivalUtc = LocalTime.of(11, 30),
            flightDate = flightDate2,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )

        repository.saveNormalizedFlights(listOf(flight1, flight2))

        val departuresDate1 = repository.getDeparturesForUi(flightDate1)
        assertEquals(1, departuresDate1.size)
        assertEquals("GLO-1234-2024-10-15", departuresDate1[0].flightId)

        val departuresDate2 = repository.getDeparturesForUi(flightDate2)
        assertEquals(1, departuresDate2.size)
        assertEquals("GLO-1234-2024-10-16", departuresDate2[0].flightId)

        // Test duplicate prevention / upsert behavior for same flightId
        val updatedFlight1 = flight1.copy(sirosStatus = "Confirmado")
        repository.saveNormalizedFlights(listOf(updatedFlight1))

        val retrieved = repository.getFlightById("GLO-1234-2024-10-15")
        assertNotNull(retrieved)
        assertEquals("Confirmado", retrieved?.sirosStatus)
    }

    @Test
    fun testClearAndOldFlightsDeletion() = runBlocking {
        val flightDate = LocalDate.of(2024, 10, 15)
        val flight = NormalizedFlight(
            flightId = "TAM-3500-2024-10-15",
            sirosRecordId = "TAM-001",
            airline = "LATAM",
            flightNumber = "3500",
            equipment = "A320",
            origin = "SBGR",
            originName = "GUARULHOS",
            originTimezone = "America/Sao_Paulo",
            destination = "SBCG",
            destinationName = "CAMPO GRANDE",
            destinationTimezone = "America/Campo_Grande",
            operationType = OperationType.ARRIVAL,
            scheduledDepartureUtc = LocalTime.of(8, 0),
            scheduledArrivalUtc = LocalTime.of(9, 15),
            flightDate = flightDate,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )

        repository.saveNormalizedFlights(listOf(flight))
        assertNotNull(repository.getFlightById("TAM-3500-2024-10-15"))

        repository.deleteOldFlights(LocalDate.of(2024, 10, 16))
        assertNull(repository.getFlightById("TAM-3500-2024-10-15"))
    }
}