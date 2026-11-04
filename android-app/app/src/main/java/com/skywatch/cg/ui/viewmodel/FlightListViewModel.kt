package com.skywatch.cg.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.db.SyncMetadata
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.SirosRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface FlightUiState {
    object Loading : FlightUiState
    data class Success(
        val arrivals: List<NormalizedFlight>,
        val departures: List<NormalizedFlight>,
        val selectedDate: LocalDate
    ) : FlightUiState
    data class Error(val message: String) : FlightUiState
    data class Refreshing(
        val arrivals: List<NormalizedFlight>,
        val departures: List<NormalizedFlight>,
        val selectedDate: LocalDate
    ) : FlightUiState
}

class FlightListViewModel(
    application: Application,
    internal val repository: SirosRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : AndroidViewModel(application) {

    // Construtor de produção (usa banco real)
    constructor(application: Application) : this(
        application,
        SirosRepository(AppDatabase.getDatabase(application))
    )

    private val _selectedDate = MutableStateFlow(todayProvider())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Flows from repository for reactive updates
    private val arrivalsFlow = _selectedDate.flatMapLatest { date ->
        repository.getArrivalsForUiFlow(date)
    }.distinctUntilChanged()

    private val departuresFlow = _selectedDate.flatMapLatest { date ->
        repository.getDeparturesForUiFlow(date)
    }.distinctUntilChanged()

    private val syncMetadataFlow = repository.getSyncMetadataFlow().distinctUntilChanged()

    private val _uiState = MutableStateFlow<FlightUiState>(FlightUiState.Loading)
    val uiState: StateFlow<FlightUiState> = _uiState.asStateFlow()

    val syncMetadata: StateFlow<SyncMetadata?> = syncMetadataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private var loadJob: Job? = null

    init {
        Log.d("FlightListViewModelDiag", "ViewModel created. Starting reactive flows.")
        observeFlights()
        // loadSyncMetadata() no longer needed - using Flow
    }

    private fun observeFlights() {
        viewModelScope.launch {
            combine(arrivalsFlow, departuresFlow, _selectedDate) { arrivals, departures, date ->
                FlightUiState.Success(arrivals, departures, date)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun loadSyncMetadata() {
        // Deprecated: using Flow now
    }

    fun loadFlights(date: LocalDate = _selectedDate.value) {
        val previousJobActive = loadJob?.isActive == true
        Log.d("FlightListViewModelDiag", "loadFlights called with date: $date. Previous job active: $previousJobActive")
        loadJob?.cancel()
        Log.d("FlightListViewModelDiag", "Previous job cancelled (if any). Updating selectedDate to: $date")
        _selectedDate.value = date
    }

    fun refresh() {
        Log.d("FlightListViewModelDiag", "refresh() called with current selectedDate: ${_selectedDate.value}")
        viewModelScope.launch {
            // Mostra estado de atualizando se já temos dados
            val currentState = _uiState.value
            if (currentState is FlightUiState.Success) {
                _uiState.value = FlightUiState.Refreshing(
                    arrivals = currentState.arrivals,
                    departures = currentState.departures,
                    selectedDate = currentState.selectedDate
                )
            } else if (currentState is FlightUiState.Refreshing) {
                // Já está atualizando, mantém
            } else {
                _uiState.value = FlightUiState.Loading
            }

            val result = repository.syncFromRemote()
            loadSyncMetadata()
            loadFlights(_selectedDate.value)
            
            // O estado final será definido por loadFlights (Success ou Error)
            // Mas podemos logar o resultado da sync
            when (result) {
                is SirosRepository.SyncResult.Success -> {
                    Log.d("FlightListViewModel", "Sync manual bem-sucedida: ${result.totalSynced} voos")
                }
                is SirosRepository.SyncResult.Failure -> {
                    Log.w("FlightListViewModel", "Sync manual falhou: ${result.errorMessage}")
                }
            }
        }
    }

    fun setDate(date: LocalDate) {
        Log.d("FlightListViewModelDiag", "setDate() called with date: $date")
        loadFlights(date)
    }

    fun goToPreviousDay() {
        val newDate = _selectedDate.value.minusDays(1)
        Log.d("FlightListViewModelDiag", "goToPreviousDay() -> $newDate")
        loadFlights(newDate)
    }

    fun goToToday() {
        val today = todayProvider()
        Log.d("FlightListViewModelDiag", "goToToday() -> $today")
        loadFlights(today)
    }

    fun goToNextDay() {
        val newDate = _selectedDate.value.plusDays(1)
        Log.d("FlightListViewModelDiag", "goToNextDay() -> $newDate")
        loadFlights(newDate)
    }
}
