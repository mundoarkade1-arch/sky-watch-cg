package com.skywatch.cg.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.radar.RadarAircraft
import com.skywatch.cg.data.radar.RadarSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RadarViewModelTest {
    private lateinit var dispatcher: TestDispatcher
    private val viewModels = mutableListOf<RadarViewModel>()

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun alertAppearsForArrivalObservation() = runTest(dispatcher) {
        val viewModel = viewModel(listOf(snapshot(1_000L, aircraft = true)))

        viewModel.refresh()
        val state = viewModel.uiState.first { it.snapshot != null }

        assertNotNull(state.radarAlert)
        assertEquals(1, state.radarAlerts.size)
        assertNotNull(state.radarAnnouncement)
    }

    @Test
    fun sameObservationIsDeduplicatedButRemainsVisible() = runTest(dispatcher) {
        val viewModel = viewModel(
            listOf(snapshot(1_000L, aircraft = true), snapshot(2_000L, aircraft = true))
        )

        viewModel.refresh()
        viewModel.uiState.first { it.snapshot?.observedAtMillis == 1_000L }
        viewModel.refresh()
        val state = viewModel.uiState.first { it.snapshot?.observedAtMillis == 2_000L }

        assertNotNull(state.radarAlert)
        assertNull(state.radarAnnouncement)
    }

    @Test
    fun alertDisappearsWhenAircraftLeavesSnapshot() = runTest(dispatcher) {
        val viewModel = viewModel(
            listOf(snapshot(1_000L, aircraft = true), snapshot(2_000L, aircraft = false))
        )

        viewModel.refresh()
        viewModel.uiState.first { it.snapshot?.observedAtMillis == 1_000L }
        viewModel.refresh()
        val state = viewModel.uiState.first { it.snapshot?.observedAtMillis == 2_000L }

        assertNull(state.radarAlert)
        assertEquals(0, state.radarAlerts.size)
    }

    private fun viewModel(snapshots: List<RadarSnapshot>): RadarViewModel {
        val remaining = snapshots.toMutableList()
        return RadarViewModel(
            ApplicationProvider.getApplicationContext<Context>() as android.app.Application,
            snapshotProvider = { remaining.removeAt(0) }
        ).also(viewModels::add)
    }

    private fun snapshot(now: Long, aircraft: Boolean): RadarSnapshot = RadarSnapshot(
        observedAtMillis = now,
        aircraft = if (aircraft) {
            listOf(
                RadarAircraft(
                    icaoHex = "abc123",
                    callsign = "TEST",
                    latitude = -20.4,
                    longitude = -54.6,
                    altitudeFeet = 8_000,
                    barometricRateFeetPerMinute = -500,
                    groundSpeedKnots = 200.0,
                    trackDegrees = 180.0,
                    secondsSincePosition = 0.0,
                    distanceNauticalMiles = 12.0,
                    bearingDegrees = 0.0
                )
            )
        } else {
            emptyList()
        },
        sourceName = "teste"
    )
}
