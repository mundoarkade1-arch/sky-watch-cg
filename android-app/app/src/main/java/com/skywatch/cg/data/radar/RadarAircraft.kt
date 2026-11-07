package com.skywatch.cg.data.radar

data class RadarAircraft(
    val icaoHex: String,
    val callsign: String?,
    val latitude: Double,
    val longitude: Double,
    val altitudeFeet: Int?,
    val barometricRateFeetPerMinute: Int?,
    val groundSpeedKnots: Double?,
    val trackDegrees: Double?,
    val secondsSincePosition: Double?,
    val distanceNauticalMiles: Double,
    val bearingDegrees: Double,
    val geometricRateFeetPerMinute: Int? = null,
    val emitterCategory: String? = null
)

data class RadarSnapshot(
    val observedAtMillis: Long,
    val aircraft: List<RadarAircraft>,
    val sourceName: String = "desconhecida"
)
