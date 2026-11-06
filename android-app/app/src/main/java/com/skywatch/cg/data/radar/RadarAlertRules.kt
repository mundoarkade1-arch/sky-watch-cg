package com.skywatch.cg.data.radar

import kotlin.math.abs
import kotlin.math.min

enum class RadarAlertType { ARRIVAL, DEPARTURE }

data class RadarAlert(
    val type: RadarAlertType,
    val distanceNauticalMiles: Double,
    val estimatedMinutes: Int?
)

object RadarAlertRules {
    const val ARRIVAL_MAX_DISTANCE_NM = 25.0
    const val DEPARTURE_MAX_DISTANCE_NM = 8.0
    const val ARRIVAL_HEADING_TOLERANCE_DEGREES = 60.0
    const val ARRIVAL_DESCENT_RATE_FPM = -300
    const val ARRIVAL_MAX_ALTITUDE_FEET = 10_000
    const val DEPARTURE_CLIMB_RATE_FPM = 500
    const val DEPARTURE_MAX_ALTITUDE_FEET = 6_000
    const val HIGH_PASSING_ALTITUDE_FEET = 20_000
    const val ALERT_COOLDOWN_MILLIS = 20 * 60 * 1_000L

    fun evaluate(aircraft: RadarAircraft): RadarAlert? {
        val altitude = aircraft.altitudeFeet ?: return null
        if (altitude > HIGH_PASSING_ALTITUDE_FEET) return null
        val rate = aircraft.barometricRateFeetPerMinute ?: return null
        if (aircraft.groundSpeedKnots == null || aircraft.groundSpeedKnots <= 0.0) return null

        if (aircraft.distanceNauticalMiles <= ARRIVAL_MAX_DISTANCE_NM &&
            rate <= ARRIVAL_DESCENT_RATE_FPM &&
            altitude <= ARRIVAL_MAX_ALTITUDE_FEET &&
            headingPointsToAirport(aircraft)
        ) {
            return RadarAlert(RadarAlertType.ARRIVAL, aircraft.distanceNauticalMiles, estimateMinutes(aircraft))
        }
        if (aircraft.distanceNauticalMiles <= DEPARTURE_MAX_DISTANCE_NM &&
            rate >= DEPARTURE_CLIMB_RATE_FPM && altitude <= DEPARTURE_MAX_ALTITUDE_FEET
        ) {
            return RadarAlert(RadarAlertType.DEPARTURE, aircraft.distanceNauticalMiles, null)
        }
        return null
    }

    private fun headingPointsToAirport(aircraft: RadarAircraft): Boolean {
        val track = aircraft.trackDegrees ?: return false
        val target = (aircraft.bearingDegrees + 180.0) % 360.0
        val delta = abs(track - target)
        return min(delta, 360.0 - delta) <= ARRIVAL_HEADING_TOLERANCE_DEGREES
    }

    private fun estimateMinutes(aircraft: RadarAircraft): Int? =
        aircraft.groundSpeedKnots?.takeIf { it > 0.0 }
            ?.let { (aircraft.distanceNauticalMiles / it * 60.0).toInt().coerceAtLeast(1) }
}

class RadarAlertDeduplicator(private val clockMillis: () -> Long = System::currentTimeMillis) {
    private val lastAlertAt = mutableMapOf<String, Long>()

    fun shouldAlert(hex: String, type: RadarAlertType): Boolean {
        val key = "$hex:${type.name}"
        val now = clockMillis()
        val previous = lastAlertAt[key]
        if (previous != null && now - previous < RadarAlertRules.ALERT_COOLDOWN_MILLIS) return false
        lastAlertAt[key] = now
        return true
    }
}
