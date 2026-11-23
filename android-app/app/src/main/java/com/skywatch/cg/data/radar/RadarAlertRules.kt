package com.skywatch.cg.data.radar

import kotlin.math.abs
import kotlin.math.min

enum class RadarAlertType { ARRIVAL, DEPARTURE }

data class RadarAlert(
    val type: RadarAlertType,
    val distanceNauticalMiles: Double,
    val estimatedMinutes: Int?,
    val bearingDegrees: Double = 0.0,
    val runway: String? = null,
    val directionDegrees: Double = bearingDegrees,
    val commercialMatch: CommercialMatchStatus = CommercialMatchStatus.UNKNOWN,
    val plannedRoute: String? = null
) {
    val lookDirection: String
        get() = directionForBearing(directionDegrees)
}

fun directionForBearing(bearingDegrees: Double): String {
    val directions = listOf("N", "NE", "L", "SE", "S", "SO", "O", "NO")
    val normalized = ((bearingDegrees % 360.0) + 360.0) % 360.0
    return directions[((normalized + 22.5) / 45.0).toInt() % directions.size]
}

object RadarAlertRules {
    const val MAX_POSITION_AGE_SECONDS = 60.0
    const val MIN_MOVING_GROUND_SPEED_KNOTS = 60.0
    const val HELICOPTER_EMITTER_CATEGORY = "A7"
    const val ARRIVAL_MAX_DISTANCE_NM = 15.0
    const val DEPARTURE_MAX_DISTANCE_NM = 6.0
    const val RUNWAY_HEADING_TOLERANCE_DEGREES = 25.0
    const val APPROACH_AXIS_TOLERANCE_DEGREES = 35.0
    const val ARRIVAL_DESCENT_RATE_FPM = -300
    const val ARRIVAL_MAX_AGL_FEET = 5_000
    const val DEPARTURE_CLIMB_RATE_FPM = 500
    const val DEPARTURE_MAX_AGL_FEET = 3_500
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
        val speed = aircraft.groundSpeedKnots ?: return null
        val rate = aircraft.barometricRateFeetPerMinute
            ?: aircraft.geometricRateFeetPerMinute
            ?: trend?.verticalRateFeetPerMinute
            ?: return null

        if (aircraft.distanceNauticalMiles <= ARRIVAL_MAX_DISTANCE_NM &&
            rate <= ARRIVAL_DESCENT_RATE_FPM &&
            agl <= ARRIVAL_MAX_AGL_FEET &&
            speed >= MIN_MOVING_GROUND_SPEED_KNOTS &&
            trendAllowsArrival(trend)
        ) {
            val runway = matchingRunway(aircraft, arrival = true) ?: return@evaluate null
            return RadarAlert(
                RadarAlertType.ARRIVAL,
                aircraft.distanceNauticalMiles,
                estimateMinutes(aircraft),
                aircraft.bearingDegrees,
                runway,
                aircraft.bearingDegrees
            )
        }
        if (aircraft.distanceNauticalMiles <= DEPARTURE_MAX_DISTANCE_NM &&
            agl <= DEPARTURE_MAX_AGL_FEET &&
            (rate >= DEPARTURE_CLIMB_RATE_FPM ||
                (agl <= 100 && speed >= 80.0 && (aircraft.groundSpeedChangeKnotsPerMinute ?: 0.0) > 0.0))
        ) {
            val runway = matchingRunway(aircraft, arrival = false) ?: return@evaluate null
            val direction = aircraft.trackDegrees ?: return@evaluate null
            val movingAway = trend == null || trend.samples.size < 2 || trend.distanceTrend == RadarDistanceTrend.RECEDING
            val onRunway = agl <= 100 && isOnRunwayAxis(aircraft, direction)
            if (!movingAway && !onRunway) return null
            return RadarAlert(
                RadarAlertType.DEPARTURE,
                aircraft.distanceNauticalMiles,
                estimateMinutes(aircraft),
                aircraft.bearingDegrees,
                runway,
                direction
            )
        }
        return null
    }

    private fun matchingRunway(aircraft: RadarAircraft, arrival: Boolean): String? {
        val track = aircraft.trackDegrees ?: return null
        val candidates = listOf(
            "06" to RadarAirportConfig.RUNWAY_06_TRUE_HEADING_DEGREES,
            "24" to RadarAirportConfig.RUNWAY_24_TRUE_HEADING_DEGREES
        )
        return candidates.firstOrNull { (_, heading) ->
            val aligned = angularDistance(track, heading) <= RUNWAY_HEADING_TOLERANCE_DEGREES
            val expectedBearing = if (arrival) (heading + 180.0) % 360.0 else (track + 180.0) % 360.0
            aligned && angularDistance(aircraft.bearingDegrees, expectedBearing) <= APPROACH_AXIS_TOLERANCE_DEGREES
        }?.first
    }

    private fun isOnRunwayAxis(aircraft: RadarAircraft, track: Double): Boolean =
        angularDistance(aircraft.bearingDegrees, (track + 180.0) % 360.0) <= APPROACH_AXIS_TOLERANCE_DEGREES

    private fun trendAllowsArrival(trend: RadarAircraftTrend?): Boolean =
        trend == null || (trend.samples.size < 2 && trend.distanceTrend == RadarDistanceTrend.UNKNOWN) || trend.distanceTrend == RadarDistanceTrend.APPROACHING

    private fun angularDistance(first: Double, second: Double): Double {
        val delta = abs(first - second) % 360.0
        return min(delta, 360.0 - delta)
    }

    private fun estimateMinutes(aircraft: RadarAircraft): Int? =
        aircraft.groundSpeedKnots?.takeIf { it > 0.0 }
            ?.let { (aircraft.distanceNauticalMiles / it * 60.0).toInt().coerceAtLeast(1) }
}

class RadarAlertHysteresis {
    fun retain(
        aircraft: RadarAircraft,
        candidate: RadarAlert?,
        previous: RadarAlert?
    ): RadarAlert? {
        if (candidate != null || previous == null) return candidate
        val agl = (aircraft.altitudeFeet ?: return null) - RadarAirportConfig.ELEVATION_FEET
        val (distanceLimit, altitudeLimit) = when (previous.type) {
            RadarAlertType.ARRIVAL ->
                RadarAlertRules.ARRIVAL_MAX_DISTANCE_NM + 2.0 to
                    RadarAlertRules.ARRIVAL_MAX_AGL_FEET + 1_000
            RadarAlertType.DEPARTURE ->
                RadarAlertRules.DEPARTURE_MAX_DISTANCE_NM + 1.0 to
                    RadarAlertRules.DEPARTURE_MAX_AGL_FEET + 1_000
        }
        return previous.takeIf {
            aircraft.distanceNauticalMiles <= distanceLimit && agl <= altitudeLimit
        }
    }
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
