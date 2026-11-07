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
import com.skywatch.cg.data.radar.RadarAlertHysteresis
import com.skywatch.cg.data.radar.RadarAircraftHistory
import com.skywatch.cg.data.radar.RadarSurveillance
import com.skywatch.cg.data.prefs.AppPreferences
import androidx.lifecycle.Lifecycle
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
    val radarAlert: RadarAlert? = null,
    val radarAlerts: List<RadarAlert> = emptyList(),
    val radarAnnouncement: RadarAlert? = null
)

class RadarViewModel(
    application: Application,
    private val repository: RadarRepository = RadarRepository(),
    private val snapshotProvider: suspend () -> RadarSnapshot = { repository.refresh() }
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, RadarRepository())

    private val mutableUiState = MutableStateFlow(RadarUiState())
    private val alertDeduplicator = RadarAlertDeduplicator()
    private val alertHysteresis = RadarAlertHysteresis()
    private val aircraftHistory = RadarAircraftHistory()
    private val activeAlerts = mutableMapOf<String, RadarAlert>()
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
                val snapshot = snapshotProvider()
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
            RadarSurveillance(repository).runForLifecycle(
                lifecycle = lifecycle,
                isEnabled = { AppPreferences.getInstance(getApplication()).radarWatchEnabled },
                onSnapshot = { snapshot, _ -> applySnapshot(snapshot) },
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

    private suspend fun applySnapshot(snapshot: RadarSnapshot) {
        val candidates = snapshot.aircraft.mapNotNull { aircraft ->
            val trend = aircraftHistory.observe(aircraft, snapshot.observedAtMillis)
            val candidate = RadarAlertRules.evaluate(aircraft, trend)
            val active = alertHysteresis.retain(aircraft, candidate, activeAlerts[aircraft.icaoHex])
            active?.let { aircraft.icaoHex to (it to candidate) }
        }
        activeAlerts.clear()
        activeAlerts.putAll(candidates.associate { it.first to it.second.first })
        val active = activeAlerts.values.toList()
        val announcement = candidates.asSequence()
            .filter { it.second.second != null }
            .map { (hex, pair) -> pair.second!!.takeIf { alertDeduplicator.shouldAlert(hex, it.type) } }
            .filterNotNull()
            .firstOrNull()
        mutableUiState.value = RadarUiState(
            snapshot = snapshot,
            radarAlert = active.firstOrNull(),
            radarAlerts = active,
            radarAnnouncement = announcement
        )
    }
}
