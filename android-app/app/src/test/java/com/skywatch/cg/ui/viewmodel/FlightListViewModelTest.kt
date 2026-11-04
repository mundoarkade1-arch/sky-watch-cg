package com.skywatch.cg.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FlightListViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: FlightListViewModel
    private lateinit var testDispatcher: TestDispatcher
    private val fixedToday = LocalDate.of(2024, 10, 15)

    @Before
    fun setUp() {
        org.robolectric.shadows.ShadowLog.stream = System.out
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val testRepository = com.skywatch.cg.data.siros.SirosRepository(db, testDispatcher)
        // Use fixed date provider for deterministic tests
        viewModel = FlightListViewModel(
            ApplicationProvider.getApplicationContext(),
            testRepository,
            { fixedToday }
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsLoading() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertTrue(state is FlightUiState.Loading)
    }

    @Test
    fun testLoadFlightsEmptyDatabase() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2024, 10, 15)
        
        // Trigger load
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val finalState = viewModel.uiState.value
        
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success)
        val success = finalState as FlightUiState.Success
        assertTrue(success.arrivals.isEmpty())
        assertTrue(success.departures.isEmpty())
        assertEquals(testDate, success.selectedDate)
    }

    @Test
    fun testLoadFlightsWithData() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2024, 10, 15)
        
        // Arrival: 09:30 UTC at Campo Grande (UTC-4) = 05:30 local on 2024-10-15
        // arrivalDate must match local date of arrival (2024-10-15), not UTC date
        val arrival = NormalizedFlight(
            flightId = "GLO-1234-2024-10-15",
            sirosRecordId = "GLO-001",
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBGR",
            originName = "GUARULHOS",
            originTimezone = "America/Sao_Paulo",
            destination = "SBCG",
            destinationName = "CAMPO GRANDE",
            destinationTimezone = "America/Campo_Grande",
            operationType = OperationType.ARRIVAL,
            scheduledDepartureUtc = LocalTime.of(8, 0),
            scheduledArrivalUtc = LocalTime.of(9, 30),
            flightDate = testDate,
            arrivalDate = testDate,  // Fixed: local arrival date is 2024-10-15
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        
        // Departure: 10:00 UTC from Campo Grande (UTC-4) = 06:00 local on 2024-10-15
        val departure = NormalizedFlight(
            flightId = "GLO-1235-2024-10-15",
            sirosRecordId = "GLO-002",
            airline = "GOL",
            flightNumber = "1235",
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
            flightDate = testDate,
            arrivalDate = testDate,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        
        // Use the SAME repository as the viewModel to ensure data visibility
        val testRepository = viewModel.repository
        testRepository.saveNormalizedFlights(listOf(arrival, departure))
        testDispatcher.scheduler.advanceUntilIdle()  // Ensure DB write completes
        
        // Trigger load
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Wait for actual data (not just non-Loading state)
        val success = viewModel.uiState
            .dropWhile { it is FlightUiState.Loading }
            .first { it is FlightUiState.Success && (it as FlightUiState.Success).arrivals.size + (it as FlightUiState.Success).departures.size >= 2 }
            as FlightUiState.Success
        
        // Consulta todos os voos gravados no banco para diagnóstico
        val allFlightsInDb = testRepository.getAllDepartures() + testRepository.getAllArrivals()
        val diagInfo = buildString {
            append("DIAGNOSTICO: ")
            append("selectedDate=$testDate; ")
            append("allFlightsDb=[${allFlightsInDb.joinToString { f -> 
                "id=${f.flightId}, flightDate=${f.flightDate}, arrivalDate=${f.arrivalDate}, depUtc=${f.scheduledDepartureUtc}, arrUtc=${f.scheduledArrivalUtc}, origTz=${f.originTimezone}, destTz=${f.destinationTimezone}"
            }}]; ")
            append("returnedArrivals=${success.arrivals.map { it.flightId }}; ")
            append("returnedDepartures=${success.departures.map { it.flightId }}")
        }
        
        assertEquals(diagInfo, 1, success.arrivals.size)
        assertEquals(diagInfo, 1, success.departures.size)
        assertEquals("GOL", success.arrivals[0].airline)
        assertEquals("1234", success.arrivals[0].flightNumber)
        assertEquals("GOL", success.departures[0].airline)
        assertEquals("1235", success.departures[0].flightNumber)
    }

    @Test
    fun testRefreshStateEmitsRefreshing() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2024, 10, 15)
        val testRemoteSource = com.skywatch.cg.data.siros.SirosRemoteSource("http://localhost:0/dummy.csv")
        val testRepository = com.skywatch.cg.data.siros.SirosRepository(db, testDispatcher, testRemoteSource)
        viewModel = FlightListViewModel(ApplicationProvider.getApplicationContext(), testRepository)

        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }

        // Trigger refresh
        viewModel.refresh()
        testDispatcher.scheduler.runCurrent()
        val refreshingState = viewModel.uiState.value
        assertTrue(refreshingState is FlightUiState.Refreshing)
        
        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = viewModel.uiState.value
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error || finalState is FlightUiState.Refreshing)
    }



    @Test
    fun testRefreshCallsLoadFlights() = runTest(testDispatcher) {
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val finalState = viewModel.uiState.value
        
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
    }

    @Test
    fun testRefreshWithSuccessEndsWithIsRefreshingFalse() = runTest(testDispatcher) {
        // Pre-populate with some data so we start in Success state
        val testDate = LocalDate.of(2024, 10, 15)
        val arrival = NormalizedFlight(
            flightId = "GLO-1111-2024-10-15",
            sirosRecordId = "GLO-111",
            airline = "GOL",
            flightNumber = "1111",
            equipment = "B738",
            origin = "SBCG",
            originName = "CAMPO GRANDE",
            originTimezone = "America/Campo_Grande",
            destination = "SBGR",
            destinationName = "GUARULHOS",
            destinationTimezone = "America/Sao_Paulo",
            operationType = OperationType.ARRIVAL,
            scheduledDepartureUtc = LocalTime.of(12, 0),
            scheduledArrivalUtc = LocalTime.of(13, 30),
            flightDate = testDate,
            arrivalDate = testDate,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        val testRepository = viewModel.repository
        testRepository.saveNormalizedFlights(listOf(arrival))
        testDispatcher.scheduler.advanceUntilIdle()

        // Load initial data to get into Success state
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify we're in Success state
        var state = viewModel.uiState.value
        assertTrue(state is FlightUiState.Success)

        // Now call refresh - repository will sync (real HTTP call in test hanging or failing)
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.value
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
        assertFalse(finalState is FlightUiState.Refreshing)
    }

    @Test
    fun testRefreshWithFailureEndsWithIsRefreshingFalse() = runTest(testDispatcher) {
        // Start with empty database (no data)
        val testDate = LocalDate.of(2024, 10, 15)
        
        // Load initial data (will be empty Success)
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify we're in Success state (empty)
        var state = viewModel.uiState.value
        assertTrue(state is FlightUiState.Success)
        val success = state as FlightUiState.Success
        assertTrue(success.arrivals.isEmpty() && success.departures.isEmpty())

        // Now call refresh - sync will fail
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.value
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
        assertFalse(finalState is FlightUiState.Refreshing)
    }

    @Test
    fun testGoToPreviousDayChangesDateAndLoadsFlights() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2024, 10, 15)
        
        // First load initial date
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val initialDate = viewModel.selectedDate.value
        assertEquals(testDate, initialDate)
        
        // Navigate to previous day
        viewModel.goToPreviousDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val prevDate = viewModel.selectedDate.value
        assertEquals(testDate.minusDays(1), prevDate)
        
        val finalState = viewModel.uiState.value
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
        if (finalState is FlightUiState.Success) {
            assertEquals(testDate.minusDays(1), finalState.selectedDate)
        }
    }

    @Test
    fun testGoToTodayChangesDateAndLoadsFlights() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2024, 10, 15)
        
        // First load a different date
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        // Navigate to today
        viewModel.goToToday()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val today = viewModel.selectedDate.value
        assertEquals(fixedToday, today)
        
        val finalState = viewModel.uiState.value
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
        if (finalState is FlightUiState.Success) {
            assertEquals(fixedToday, finalState.selectedDate)
        }
    }

    @Test
    fun testGoToNextDayChangesDateAndLoadsFlights() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2024, 10, 15)
        
        // First load initial date
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val initialDate = viewModel.selectedDate.value
        assertEquals(testDate, initialDate)
        
        // Navigate to next day
        viewModel.goToNextDay()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val nextDate = viewModel.selectedDate.value
        assertEquals(testDate.plusDays(1), nextDate)
        
        val finalState = viewModel.uiState.value
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
        if (finalState is FlightUiState.Success) {
            assertEquals(testDate.plusDays(1), finalState.selectedDate)
        }
    }

    @Test
    fun testUtcDayRolloverFlightAppearsOnLocalDate() = runTest(testDispatcher) {
        // Flight at 02:00 UTC on 2024-10-15 = 22:00 on 2024-10-14 in UTC-4 (America/Campo_Grande)
        // Should appear on LOCAL date 2024-10-14, not 2024-10-15
        val utcFlightDate = LocalDate.of(2024, 10, 15)
        val localExpectedDate = LocalDate.of(2024, 10, 14)
        
        val departureAt0200Utc = NormalizedFlight(
            flightId = "GLO-9999-2024-10-15",
            sirosRecordId = "GLO-999",
            airline = "GOL",
            flightNumber = "9999",
            equipment = "B738",
            origin = "SBCG",
            originName = "CAMPO GRANDE",
            originTimezone = "America/Campo_Grande", // UTC-4
            destination = "SBGR",
            destinationName = "GUARULHOS",
            destinationTimezone = "America/Sao_Paulo",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(2, 0), // 02:00 UTC
            scheduledArrivalUtc = LocalTime.of(3, 30),
            flightDate = utcFlightDate,  // UTC date of departure
            arrivalDate = utcFlightDate,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        
        // Use the SAME repository instance as the viewModel to ensure data visibility
        val testRepository = viewModel.repository
        testRepository.saveNormalizedFlights(listOf(departureAt0200Utc))
        testDispatcher.scheduler.advanceUntilIdle()  // Ensure DB write completes
        
        // Load flights for the LOCAL date (14th) - should include the 02:00 UTC flight
        viewModel.loadFlights(localExpectedDate)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Wait for actual data (not just non-Loading state)
        val success = viewModel.uiState
            .dropWhile { it is FlightUiState.Loading }
            .first { it is FlightUiState.Success && (it as FlightUiState.Success).departures.size >= 1 }
            as FlightUiState.Success
        
        // Consulta todos os voos gravados no banco para diagnóstico completo
        val allFlightsInDb = testRepository.getAllDepartures() + testRepository.getAllArrivals()
        val diagInfo = buildString {
            append("DIAGNOSTICO: ")
            append("selectedDate=$localExpectedDate; ")
            append("allFlightsDb=[${allFlightsInDb.joinToString { f -> 
                "id=${f.flightId}, flightDate=${f.flightDate}, arrivalDate=${f.arrivalDate}, depUtc=${f.scheduledDepartureUtc}, arrUtc=${f.scheduledArrivalUtc}, origTz=${f.originTimezone}, destTz=${f.destinationTimezone}"
            }}]; ")
            append("returnedArrivals=${success.arrivals.map { it.flightId }}; ")
            append("returnedDepartures=${success.departures.map { it.flightId }}")
        }

        // Flight should appear on 14th (local date), not 15th
        assertEquals(diagInfo, localExpectedDate, success.selectedDate)
        assertEquals(diagInfo, 1, success.departures.size)
        assertEquals("GLO-9999-2024-10-15", success.departures[0].flightId)
        assertEquals(LocalTime.of(2, 0), success.departures[0].scheduledDepartureUtc)
    }
}