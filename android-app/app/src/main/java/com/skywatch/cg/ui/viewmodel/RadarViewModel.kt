package com.skywatch.cg.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skywatch.cg.data.radar.RadarRepository
import com.skywatch.cg.data.radar.RadarSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class RadarUiState(
    val isLoading: Boolean = false,
    val snapshot: RadarSnapshot? = null,
    val errorMessage: String? = null
)

class RadarViewModel(
    application: Application,
    private val repository: RadarRepository = RadarRepository()
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, RadarRepository())

    private val mutableUiState = MutableStateFlow(RadarUiState())
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
                mutableUiState.value = RadarUiState(snapshot = snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Não foi possível consultar a fonte ADS-B."
                )
            }
        }
    }
}
