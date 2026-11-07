package com.skywatch.cg.data.radar

import kotlin.math.abs

private const val MAX_HISTORY_SAMPLES = 5
private const val MIN_TREND_INTERVAL_MILLIS = 1_000L
private const val TREND_RATE_DEADBAND_FPM = 100
private const val DISTANCE_TREND_DEADBAND_NM = 0.1

data class RadarAircraftSample(
    val observedAtMillis: Long,
    val distanceNauticalMiles: Double,
    val altitudeFeet: Int?,
    val verticalRateFeetPerMinute: Int?
)

enum class RadarVerticalTrend { ASCENDING, DESCENDING, LEVEL, UNKNOWN }
enum class RadarDistanceTrend { APPROACHING, RECEDING, STABLE, UNKNOWN }

data class RadarAircraftTrend(
    val samples: List<RadarAircraftSample>,
    val distanceDeltaNauticalMiles: Double?,
    val altitudeDeltaFeet: Int?,
    val verticalRateFeetPerMinute: Int?,
    val verticalTrend: RadarVerticalTrend,
    val distanceTrend: RadarDistanceTrend
)

/** Mantém somente um histórico curto por hex ICAO e deriva tendências das amostras. */
class RadarAircraftHistory(private val maxSamples: Int = MAX_HISTORY_SAMPLES) {
    private val samplesByHex = mutableMapOf<String, ArrayDeque<RadarAircraftSample>>()

    fun observe(aircraft: RadarAircraft, observedAtMillis: Long): RadarAircraftTrend {
        val sample = RadarAircraftSample(
            observedAtMillis = observedAtMillis,
            distanceNauticalMiles = aircraft.distanceNauticalMiles,
            altitudeFeet = aircraft.altitudeFeet,
            verticalRateFeetPerMinute = aircraft.barometricRateFeetPerMinute
                ?: aircraft.geometricRateFeetPerMinute
        )
        val history = samplesByHex.getOrPut(aircraft.icaoHex) { ArrayDeque() }
        if (history.lastOrNull()?.observedAtMillis != observedAtMillis) history.addLast(sample)
        while (history.size > maxSamples) history.removeFirst()
        return derive(history.toList())
    }

    fun samplesFor(hex: String): List<RadarAircraftSample> = samplesByHex[hex].orEmpty().toList()

    private fun derive(samples: List<RadarAircraftSample>): RadarAircraftTrend {
        val previous = samples.dropLast(1).lastOrNull()
        val current = samples.lastOrNull()
        if (previous == null || current == null) {
            return RadarAircraftTrend(samples, null, null, current?.verticalRateFeetPerMinute, RadarVerticalTrend.UNKNOWN, RadarDistanceTrend.UNKNOWN)
        }
        val elapsedMillis = current.observedAtMillis - previous.observedAtMillis
        val distanceDelta = current.distanceNauticalMiles - previous.distanceNauticalMiles
        val altitudeDelta = current.altitudeFeet?.let { now -> previous.altitudeFeet?.let { now - it } }
        val derivedRate = if (current.verticalRateFeetPerMinute == null && altitudeDelta != null && elapsedMillis >= MIN_TREND_INTERVAL_MILLIS) {
            (altitudeDelta * 60_000L / elapsedMillis).toInt()
        } else {
            current.verticalRateFeetPerMinute
        }
        val trendRate = derivedRate ?: return RadarAircraftTrend(samples, distanceDelta, altitudeDelta, null, RadarVerticalTrend.UNKNOWN, distanceTrend(distanceDelta))
        val verticalTrend = when {
            trendRate >= TREND_RATE_DEADBAND_FPM -> RadarVerticalTrend.ASCENDING
            trendRate <= -TREND_RATE_DEADBAND_FPM -> RadarVerticalTrend.DESCENDING
            abs(trendRate) < TREND_RATE_DEADBAND_FPM -> RadarVerticalTrend.LEVEL
            else -> RadarVerticalTrend.UNKNOWN
        }
        return RadarAircraftTrend(samples, distanceDelta, altitudeDelta, derivedRate, verticalTrend, distanceTrend(distanceDelta))
    }

    private fun distanceTrend(delta: Double) = when {
        delta <= -DISTANCE_TREND_DEADBAND_NM -> RadarDistanceTrend.APPROACHING
        delta >= DISTANCE_TREND_DEADBAND_NM -> RadarDistanceTrend.RECEDING
        else -> RadarDistanceTrend.STABLE
    }
}
