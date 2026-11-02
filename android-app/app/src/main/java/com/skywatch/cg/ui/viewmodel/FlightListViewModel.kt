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
    private val repository: SirosRepository
) : AndroidViewModel(application) {

    // Construtor de produção (usa banco real)
    constructor(application: Application) : this(
        application,
        SirosRepository(AppDatabase.getDatabase(application))
    )

    private val _uiState = MutableStateFlow<FlightUiState>(FlightUiState.Loading)
    val uiState: StateFlow<FlightUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _syncMetadata = MutableStateFlow<SyncMetadata?>(null)
    val syncMetadata: StateFlow<SyncMetadata?> = _syncMetadata.asStateFlow()

    private var loadJob: Job? = null

    init {
        Log.d("FlightListViewModelDiag", "ViewModel created. Calling init loadFlights()")
        loadFlights()
        loadSyncMetadata()
    }

    fun loadSyncMetadata() {
        viewModelScope.launch {
            try {
                _syncMetadata.value = repository.getSyncMetadata()
            } catch (e: Exception) {
                Log.e("FlightListViewModel", "Erro ao carregar sync metadata: ${e.message}")
            }
        }
    }

    fun loadFlights(date: LocalDate = _selectedDate.value) {
        val previousJobActive = loadJob?.isActive == true
        Log.d("FlightListViewModelDiag", "loadFlights called with date: $date. Previous job active: $previousJobActive")
        loadJob?.cancel()
        Log.d("FlightListViewModelDiag", "Previous job cancelled (if any). Launching new coroutine for date: $date")
        loadJob = viewModelScope.launch {
            Log.d("FlightListViewModelDiag", "Coroutine started for date: $date. Setting state to Loading.")
            _uiState.value = FlightUiState.Loading
            _selectedDate.value = date
            try {
                Log.d("FlightListViewModelDiag", "Repository fetching arrivals and departures for date: $date")
                val arrivals = repository.getArrivalsForUi(date)
                val departures = repository.getDeparturesForUi(date)
                Log.d("FlightListViewModelDiag", "Repository returned ${arrivals.size} arrivals and ${departures.size} departures for date: $date. Setting Success.")
                _uiState.value = FlightUiState.Success(
                    arrivals = arrivals,
                    departures = departures,
                    selectedDate = date
                )
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.d("FlightListViewModelDiag", "Exception caught in loadFlights for date: $date: ${e.message}. Setting Error.")
                    _uiState.value = FlightUiState.Error(e.localizedMessage ?: "Erro desconhecido")
                } else {
                    Log.d("FlightListViewModelDiag", "CancellationException caught for date: $date (job cancelled normally).")
                }
            }
        }
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
        val today = LocalDate.now()
        Log.d("FlightListViewModelDiag", "goToToday() -> $today")
        loadFlights(today)
    }

    fun goToNextDay() {
        val newDate = _selectedDate.value.plusDays(1)
        Log.d("FlightListViewModelDiag", "goToNextDay() -> $newDate")
        loadFlights(newDate)
    }
}
