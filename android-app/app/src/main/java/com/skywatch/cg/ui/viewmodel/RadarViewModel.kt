package com.skywatch.cg.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skywatch.cg.data.radar.RadarRepository
import com.skywatch.cg.data.radar.RadarSnapshot
import com.skywatch.cg.data.radar.RadarUnavailableException
import com.skywatch.cg.data.radar.RadarAlert
import com.skywatch.cg.data.radar.RadarAlertDeduplicator
import com.skywatch.cg.data.radar.RadarAlertRules
import com.skywatch.cg.data.radar.RadarAircraftHistory
import com.skywatch.cg.data.radar.RadarSurveillance
import com.skywatch.cg.data.prefs.AppPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class RadarUiState(
    val isLoading: Boolean = false,
    val snapshot: RadarSnapshot? = null,
    val errorMessage: String? = null,
    val retryAfterSeconds: Long? = null,
    val radarAlert: RadarAlert? = null
)

class RadarViewModel(
    application: Application,
    private val repository: RadarRepository = RadarRepository()
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, RadarRepository())

    private val mutableUiState = MutableStateFlow(RadarUiState())
    private val alertDeduplicator = RadarAlertDeduplicator()
    private val aircraftHistory = RadarAircraftHistory()
    private var surveillanceJob: Job? = null
    val uiState: StateFlow<RadarUiState> = mutableUiState.asStateFlow()

    fun refresh() {
        if (mutableUiState.value.isLoading) return

        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            errorMessage = null
        )
        viewModelScope.launch {
            try {
                val snapshot = repository.refresh()
                applySnapshot(snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RadarUnavailableException) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message,
                    retryAfterSeconds = e.retryAfterSeconds
                )
            } catch (e: IOException) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Não foi possível consultar as fontes ADS-B."
                )
            }
        }
    }

    fun startSurveillance(lifecycle: Lifecycle) {
        if (surveillanceJob?.isActive == true) return
        surveillanceJob = viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                RadarSurveillance(repository).run(
                    isStarted = { true },
                    isEnabled = { AppPreferences.getInstance(getApplication()).radarWatchEnabled },
                    onSnapshot = { snapshot, _ ->
                        applySnapshot(snapshot)
                    },
                    onError = { error ->
                        if (error is RadarUnavailableException || error is IOException) {
                            mutableUiState.value = mutableUiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message
                            )
                        }
                    }
                )
            }
        }
    }

    private suspend fun applySnapshot(snapshot: RadarSnapshot) {
        val detectedAlert = snapshot.aircraft.asSequence().mapNotNull { aircraft ->
            val trend = aircraftHistory.observe(aircraft, snapshot.observedAtMillis)
            RadarAlertRules.evaluate(aircraft, trend)?.takeIf {
                alertDeduplicator.shouldAlert(aircraft.icaoHex, it.type)
            }
        }.firstOrNull()
        mutableUiState.value = RadarUiState(snapshot = snapshot, radarAlert = detectedAlert)
    }
}
