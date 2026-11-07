package com.skywatch.cg.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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

sealed interface FlightDetailUiState {
    data class Loading(val flightId: String) : FlightDetailUiState
    data class Success(val flightId: String, val flight: NormalizedFlight) : FlightDetailUiState
    data class Error(val flightId: String, val message: String) : FlightDetailUiState
}

class FlightListViewModel(
    application: Application,
    internal val repository: SirosRepository,
    private val todayProvider: () -> LocalDate = {
        LocalDate.now(ZoneId.of("America/Campo_Grande"))
    }
) : AndroidViewModel(application) {

    // Construtor de produção (usa banco real)
    constructor(application: Application) : this(
        application,
        SirosRepository(AppDatabase.getDatabase(application))
    )

    private val _selectedDate = MutableStateFlow(todayProvider())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val syncMetadataFlow = repository.getSyncMetadataFlow().distinctUntilChanged()

    private val _uiState = MutableStateFlow<FlightUiState>(FlightUiState.Loading)
    val uiState: StateFlow<FlightUiState> = _uiState.asStateFlow()

    val syncMetadata: StateFlow<SyncMetadata?> = syncMetadataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _flightDetailUiState = MutableStateFlow<FlightDetailUiState?>(null)
    val flightDetailUiState: StateFlow<FlightDetailUiState?> = _flightDetailUiState.asStateFlow()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null
    private var flightDetailJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var initialSyncChecked = false
    private val _currentLocalDateTime = MutableStateFlow(LocalDateTime.now(ZoneId.of("America/Campo_Grande")))
    val currentLocalDateTime: StateFlow<LocalDateTime> = _currentLocalDateTime.asStateFlow()

    init {
        Log.d("FlightListViewModelDiag", "ViewModel created. Starting reactive flows.")
        observeFlights()
        // loadSyncMetadata() no longer needed - using Flow
    }

    private fun observeFlights() {
        viewModelScope.launch {
            _selectedDate.flatMapLatest { date ->
                combine(
                    repository.getArrivalsForUiFlow(date),
                    repository.getDeparturesForUiFlow(date)
                ) { arrivals, departures ->
                    FlightUiState.Success(arrivals, departures, date)
                }
            }.collect { state ->
                _uiState.value = when (val currentState = _uiState.value) {
                    is FlightUiState.Refreshing -> currentState.copy(
                        arrivals = state.arrivals,
                        departures = state.departures,
                        selectedDate = state.selectedDate
                    )
                    else -> state
                }
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
        if (_selectedDate.value != date) {
            _uiState.value = FlightUiState.Loading
            _selectedDate.value = date
        }
    }

    fun loadFlightDetails(flightId: String) {
        if (flightDetailJob?.isActive == true &&
            (_flightDetailUiState.value as? FlightDetailUiState.Loading)?.flightId == flightId
        ) return

        flightDetailJob?.cancel()
        _flightDetailUiState.value = FlightDetailUiState.Loading(flightId)
        flightDetailJob = viewModelScope.launch {
            try {
                val flight = repository.getFlightById(flightId)?.toNormalizedFlight()
                _flightDetailUiState.value = if (flight == null) {
                    FlightDetailUiState.Error(flightId, "Voo não encontrado no cache local.")
                } else {
                    FlightDetailUiState.Success(flightId, flight)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FlightListViewModel", "Falha ao carregar detalhes do voo $flightId", e)
                _flightDetailUiState.value = FlightDetailUiState.Error(
                    flightId,
                    e.localizedMessage ?: "Não foi possível carregar os detalhes do voo."
                )
            }
        }
    }

    fun refresh() {
        Log.d("FlightListViewModelDiag", "refresh() called with current selectedDate: ${_selectedDate.value}")
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            val currentState = _uiState.value
            val previousArrivals = when (currentState) {
                is FlightUiState.Success -> currentState.arrivals
                is FlightUiState.Refreshing -> currentState.arrivals
                else -> emptyList()
            }
            val previousDepartures = when (currentState) {
                is FlightUiState.Success -> currentState.departures
                is FlightUiState.Refreshing -> currentState.departures
                else -> emptyList()
            }
            val refreshDate = _selectedDate.value
            _uiState.value = FlightUiState.Refreshing(
                arrivals = previousArrivals,
                departures = previousDepartures,
                selectedDate = refreshDate
            )

            try {
                val result = repository.syncFromRemote()
                val arrivals = repository.getArrivalsForUi(_selectedDate.value)
                val departures = repository.getDeparturesForUi(_selectedDate.value)

                when (result) {
                    is SirosRepository.SyncResult.Success -> {
                        _uiState.value = FlightUiState.Success(
                            arrivals = arrivals,
                            departures = departures,
                            selectedDate = _selectedDate.value
                        )
                        Log.d("FlightListViewModel", "Sync manual bem-sucedida: ${result.totalSynced} voos")
                    }
                    is SirosRepository.SyncResult.Failure -> {
                        Log.w("FlightListViewModel", "Sync manual falhou: ${result.errorMessage}")
                        if (arrivals.isEmpty() && departures.isEmpty()) {
                            _uiState.value = FlightUiState.Error(result.errorMessage)
                        } else {
                            _uiState.value = FlightUiState.Success(
                                arrivals = arrivals,
                                departures = departures,
                                selectedDate = _selectedDate.value
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FlightListViewModel", "Exceção no refresh: ${e.message}", e)
                if (previousArrivals.isEmpty() && previousDepartures.isEmpty()) {
                    _uiState.value = FlightUiState.Error(e.localizedMessage ?: "Erro desconhecido")
                } else {
                    _uiState.value = FlightUiState.Success(
                        arrivals = previousArrivals,
                        departures = previousDepartures,
                        selectedDate = refreshDate
                    )
                }
            }
        }
    }

    fun setDate(date: LocalDate) {
        Log.d("FlightListViewModelDiag", "setDate() called with date: $date")
        loadFlights(date)
    }

    fun startAutoRefresh(lifecycle: Lifecycle): Job {
        if (autoRefreshJob?.isActive == true) return autoRefreshJob!!
        autoRefreshJob = viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncIfStaleOrEmpty()
                var lastKnownDate = todayProvider()
                while (isActive) {
                    _currentLocalDateTime.value = LocalDateTime.now(ZoneId.of("America/Campo_Grande"))
                    delay(60_000L)
                    _currentLocalDateTime.value = LocalDateTime.now(ZoneId.of("America/Campo_Grande"))
                    val today = todayProvider()
                    if (today != lastKnownDate) {
                        lastKnownDate = today
                        if (_selectedDate.value == lastKnownDate.minusDays(1)) {
                            _selectedDate.value = today
                        }
                    }
                }
            }
        }
        return autoRefreshJob!!
    }

    private suspend fun syncIfStaleOrEmpty() {
        if (initialSyncChecked) return
        initialSyncChecked = true

        val metadata = repository.getSyncMetadata()
        val hasFlights = repository.hasAnyFlights()
        if (shouldSyncOnOpen(
                lastSuccessfulSyncAt = metadata?.lastSuccessfulSyncAt ?: 0L,
                hasFlights = hasFlights,
                nowMillis = System.currentTimeMillis()
            )) {
            refresh()
        }
    }

    internal fun shouldSyncOnOpen(
        lastSuccessfulSyncAt: Long,
        hasFlights: Boolean,
        nowMillis: Long
    ): Boolean = !hasFlights ||
        lastSuccessfulSyncAt <= 0L ||
        nowMillis - lastSuccessfulSyncAt >= INITIAL_SYNC_MAX_AGE_MS

    private companion object {
        const val INITIAL_SYNC_MAX_AGE_MS = 6 * 60 * 60 * 1000L
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
