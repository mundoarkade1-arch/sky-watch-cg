package com.skywatch.cg.data.radar

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

enum class CommercialMatchStatus { MATCHED, NOT_MATCHED, UNKNOWN }

data class RadarCommercialMatch(
    val status: CommercialMatchStatus,
    val flight: NormalizedFlight? = null
) {
    val description: String? get() = flight?.let { "${it.airline} · ${it.origin} → ${it.destination}" }
}

/** Casa um callsign transmitido com o planejamento SIROS já disponível localmente. */
object RadarCommercialMatcher {
    private const val WINDOW_MINUTES = 90L
    private val knownPrefixes = setOf("AZU", "GLO", "TAM", "LAT", "ONE", "PTB", "VPB")

    fun match(callsign: String?, now: Instant, flights: Iterable<NormalizedFlight>): RadarCommercialMatch {
        val normalized = callsign?.trim()?.uppercase()?.replace(" ", "")
        if (normalized.isNullOrBlank()) return RadarCommercialMatch(CommercialMatchStatus.UNKNOWN)
        val prefix = knownPrefixes.firstOrNull { normalized.startsWith(it) }
            ?: return RadarCommercialMatch(CommercialMatchStatus.UNKNOWN)

        val matched = flights.asSequence()
            .filter { it.operationType == OperationType.ARRIVAL || it.operationType == OperationType.DEPARTURE }
            .filter { airlineMatches(prefix, it.airline) }
            .filter {
                val eventDate = if (it.operationType == OperationType.ARRIVAL) it.arrivalDate else it.flightDate
                val eventTime = if (it.operationType == OperationType.ARRIVAL) it.scheduledArrivalUtc else it.scheduledDepartureUtc
                val event = eventDate.atTime(eventTime).toInstant(ZoneOffset.UTC)
                kotlin.math.abs(Duration.between(now, event).toMinutes()) <= WINDOW_MINUTES
            }
            .minByOrNull {
                val eventDate = if (it.operationType == OperationType.ARRIVAL) it.arrivalDate else it.flightDate
                val eventTime = if (it.operationType == OperationType.ARRIVAL) it.scheduledArrivalUtc else it.scheduledDepartureUtc
                kotlin.math.abs(Duration.between(now, eventDate.atTime(eventTime).toInstant(ZoneOffset.UTC)).toMinutes())
            }
        return RadarCommercialMatch(if (matched == null) CommercialMatchStatus.NOT_MATCHED else CommercialMatchStatus.MATCHED, matched)
    }

    private fun airlineMatches(prefix: String, airline: String): Boolean {
        val name = airline.uppercase()
        return when (prefix) {
            "AZU" -> "AZUL" in name
            "GLO" -> "GOL" in name
            "TAM", "LAT" -> "LATAM" in name || "TAM" in name || "LAT" in name
            "ONE" -> "PASSAREDO" in name || "RIMA" in name
            "PTB", "VPB" -> "PANTANAL" in name || "PASSAREDO" in name || "VOEPASS" in name
            else -> false
        }
    }
}
