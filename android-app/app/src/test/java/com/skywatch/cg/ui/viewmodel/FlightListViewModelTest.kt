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
        viewModel = FlightListViewModel(ApplicationProvider.getApplicationContext(), testRepository)
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
    fun testRefreshCallsLoadFlights() = runTest(testDispatcher) {
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.first { it !is FlightUiState.Loading }
        
        val finalState = viewModel.uiState.value
        
        assertNotNull(finalState)
        assertTrue(finalState is FlightUiState.Success || finalState is FlightUiState.Error)
    }
}