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
    const val MAX_POSITION_AGE_SECONDS = 120.0
    const val MIN_MOVING_GROUND_SPEED_KNOTS = 5.0
    const val HELICOPTER_EMITTER_CATEGORY = "A7"
    const val ARRIVAL_MAX_DISTANCE_NM = 25.0
    const val DEPARTURE_MAX_DISTANCE_NM = 8.0
    const val ARRIVAL_HEADING_TOLERANCE_DEGREES = 60.0
    const val ARRIVAL_DESCENT_RATE_FPM = -300
    const val ARRIVAL_MAX_AGL_FEET = 10_000
    const val DEPARTURE_CLIMB_RATE_FPM = 500
    const val DEPARTURE_MAX_AGL_FEET = 6_000
    const val HIGH_PASSING_AGL_FEET = 20_000
    const val ALERT_COOLDOWN_MILLIS = 20 * 60 * 1_000L

    fun evaluate(aircraft: RadarAircraft, trend: RadarAircraftTrend? = null): RadarAlert? {
        val altitude = aircraft.altitudeFeet ?: return null
        val agl = altitude - RadarAirportConfig.ELEVATION_FEET
        if (agl > HIGH_PASSING_AGL_FEET || agl < 0) return null
        if (aircraft.secondsSincePosition == null || aircraft.secondsSincePosition > MAX_POSITION_AGE_SECONDS) return null
        if (!aircraft.latitude.isFinite() || aircraft.latitude !in -90.0..90.0 ||
            !aircraft.longitude.isFinite() || aircraft.longitude !in -180.0..180.0 ||
            !aircraft.distanceNauticalMiles.isFinite() || aircraft.distanceNauticalMiles < 0.0
        ) return null
        if (aircraft.emitterCategory == HELICOPTER_EMITTER_CATEGORY) return null
        if (aircraft.groundSpeedKnots == null || aircraft.groundSpeedKnots < MIN_MOVING_GROUND_SPEED_KNOTS) return null
        val rate = aircraft.barometricRateFeetPerMinute
            ?: aircraft.geometricRateFeetPerMinute
            ?: trend?.verticalRateFeetPerMinute
            ?: return null

        if (aircraft.distanceNauticalMiles <= ARRIVAL_MAX_DISTANCE_NM &&
            rate <= ARRIVAL_DESCENT_RATE_FPM &&
            agl <= ARRIVAL_MAX_AGL_FEET &&
            headingPointsToAirport(aircraft)
        ) {
            return RadarAlert(RadarAlertType.ARRIVAL, aircraft.distanceNauticalMiles, estimateMinutes(aircraft))
        }
        if (aircraft.distanceNauticalMiles <= DEPARTURE_MAX_DISTANCE_NM &&
            rate >= DEPARTURE_CLIMB_RATE_FPM && agl <= DEPARTURE_MAX_AGL_FEET
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
