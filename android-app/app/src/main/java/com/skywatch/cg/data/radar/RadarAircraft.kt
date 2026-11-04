package com.skywatch.cg.data.radar

data class RadarAircraft(
    val icaoHex: String,
    val callsign: String?,
    val latitude: Double,
    val longitude: Double,
    val altitudeFeet: Int?,
    val groundSpeedKnots: Double?,
    val trackDegrees: Double?,
    val secondsSincePosition: Double?,
    val distanceNauticalMiles: Double,
    val bearingDegrees: Double
)

data class RadarSnapshot(
    val observedAtMillis: Long,
    val aircraft: List<RadarAircraft>
)
