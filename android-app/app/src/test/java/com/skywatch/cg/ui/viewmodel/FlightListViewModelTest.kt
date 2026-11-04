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
        val fixedToday = LocalDate.of(2024, 10, 15)
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
        
        // Insert test data directly via DAO
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
            arrivalDate = testDate.plusDays(1),
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        
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
        
        val repository = com.skywatch.cg.data.siros.SirosRepository(db, testDispatcher)
        repository.saveNormalizedFlights(listOf(arrival, departure))
        
        // Trigger load
        viewModel.loadFlights(testDate)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val finalState = viewModel.uiState.value
        
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success)
        val success = finalState as FlightUiState.Success
        assertEquals(1, success.arrivals.size)
        assertEquals(1, success.departures.size)
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
        assertEquals(LocalDate.now(), today)
        
        val finalState = viewModel.uiState.value
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
        if (finalState is FlightUiState.Success) {
            assertEquals(LocalDate.now(), finalState.selectedDate)
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
            flightDate = utcFlightDate,
            arrivalDate = utcFlightDate,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        
        val repository = com.skywatch.cg.data.siros.SirosRepository(db, testDispatcher)
        repository.saveNormalizedFlights(listOf(departureAt0200Utc))
        
        // Load flights for the LOCAL date (14th) - should include the 02:00 UTC flight
        viewModel.loadFlights(localExpectedDate)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val finalState = viewModel.uiState.value
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success)
        val success = finalState as FlightUiState.Success
        
        // Flight should appear on 14th (local date), not 15th
        assertEquals(localExpectedDate, success.selectedDate)
        assertEquals(1, success.departures.size)
        assertEquals("GLO-9999-2024-10-15", success.departures[0].flightId)
        assertEquals(LocalTime.of(2, 0), success.departures[0].scheduledDepartureUtc)
    }
}