package com.skywatch.cg.data.radar

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object RadarSnapshotParser {
    fun parse(
        json: String,
        centerLatitude: Double,
        centerLongitude: Double
    ): RadarSnapshot {
        try {
            val root = JSONObject(json)
            val observedAtMillis = root.optLong("now", -1L)
            if (observedAtMillis <= 0L) {
                throw IOException("Resposta ADS-B sem horário válido")
            }

            val aircraftArray = root.optJSONArray("ac")
                ?: throw IOException("Resposta ADS-B sem lista de aeronaves")
            val aircraft = buildList {
                for (index in 0 until aircraftArray.length()) {
                    val item = aircraftArray.optJSONObject(index) ?: continue
                    val icaoHex = item.optString("hex").trim().lowercase()
                    val latitude = item.optionalDouble("lat")
                    val longitude = item.optionalDouble("lon")

                    if (icaoHex.isBlank() ||
                        latitude == null || latitude !in -90.0..90.0 ||
                        longitude == null || longitude !in -180.0..180.0
                    ) {
                        continue
                    }

                    val (calculatedDistance, calculatedBearing) =
                        distanceAndBearing(centerLatitude, centerLongitude, latitude, longitude)
                    val distance = item.optionalDouble("dst")
                        ?.takeIf { it >= 0.0 } ?: calculatedDistance
                    val bearing = item.optionalDouble("dir")
                        ?.takeIf { it in 0.0..360.0 } ?: calculatedBearing
                    val callsign = (item.opt("flight") as? String)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }

                    add(
                        RadarAircraft(
                            icaoHex = icaoHex,
                            callsign = callsign,
                            latitude = latitude,
                            longitude = longitude,
                            altitudeFeet = item.optionalDouble("alt_baro")
                                ?.takeIf { it >= 0.0 }
                                ?.toInt(),
                            barometricRateFeetPerMinute = item.optionalDouble("baro_rate")?.toInt(),
                            groundSpeedKnots = item.optionalDouble("gs")
                                ?.takeIf { it >= 0.0 },
                            trackDegrees = item.optionalDouble("track")
                                ?.takeIf { it in 0.0..360.0 },
                            secondsSincePosition = item.optionalDouble("seen_pos")
                                ?.takeIf { it >= 0.0 },
                            distanceNauticalMiles = distance,
                            bearingDegrees = bearing
                        )
                    )
                }
            }

            return RadarSnapshot(observedAtMillis, aircraft)
        } catch (e: JSONException) {
            throw IOException("Resposta ADS-B inválida", e)
        }
    }

    private fun JSONObject.optionalDouble(name: String): Double? {
        val value = opt(name)
        return when (value) {
            is Number -> value.toDouble().takeIf { it.isFinite() }
            is String -> value.toDoubleOrNull()?.takeIf { it.isFinite() }
            else -> null
        }
    }

    private fun distanceAndBearing(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Pair<Double, Double> {
        val earthRadiusNm = 3440.065
        val fromLatRadians = Math.toRadians(fromLatitude)
        val toLatRadians = Math.toRadians(toLatitude)
        val latitudeDelta = Math.toRadians(toLatitude - fromLatitude)
        val longitudeDelta = Math.toRadians(toLongitude - fromLongitude)

        val haversine = sin(latitudeDelta / 2).let { it * it } +
            cos(fromLatRadians) * cos(toLatRadians) *
            sin(longitudeDelta / 2).let { it * it }
        val distance = 2 * earthRadiusNm * atan2(sqrt(haversine), sqrt(1 - haversine))

        val y = sin(longitudeDelta) * cos(toLatRadians)
        val x = cos(fromLatRadians) * sin(toLatRadians) -
            sin(fromLatRadians) * cos(toLatRadians) * cos(longitudeDelta)
        val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        return distance to bearing
    }
}
