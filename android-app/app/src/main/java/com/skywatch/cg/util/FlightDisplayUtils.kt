package com.skywatch.cg.util

import com.skywatch.cg.data.siros.NormalizedFlight
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Funções de utilidade para formatação de voos.
 */
internal val campoGrandeZone = ZoneId.of("America/Campo_Grande")
internal val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Campo_Grande"))

/**
 * Retorna o horário local formatado (HH:mm) para chegada ou partida do voo.
 */
internal fun NormalizedFlight.getDisplayTime(isArrival: Boolean): String {
    val utcTime = if (isArrival) scheduledArrivalUtc else scheduledDepartureUtc
    val flightDateToUse = if (isArrival) arrivalDate else flightDate
    val flightDateTime = flightDateToUse.atTime(utcTime)
    val utcZoned = flightDateTime.atZone(ZoneId.of("UTC"))
    val localZoned = utcZoned.withZoneSameInstant(campoGrandeZone)
    return localZoned.format(timeFormatter)
}

/**
 * Retorna a data/hora local completa formatada para exibição.
 */
internal fun NormalizedFlight.getDisplayDateTime(isArrival: Boolean): String {
    val utcTime = if (isArrival) scheduledArrivalUtc else scheduledDepartureUtc
    val flightDateToUse = if (isArrival) arrivalDate else flightDate
    val flightDateTime = flightDateToUse.atTime(utcTime)
    val utcZoned = flightDateTime.atZone(ZoneId.of("UTC"))
    val localZoned = utcZoned.withZoneSameInstant(campoGrandeZone)
    return localZoned.format(localDateTimeFormatter)
}

/**
 * Retorna data/hora de sincronização formatada para exibição.
 */
internal fun Long.getDisplayLastUpdate(): String {
    return if (this > 0) {
        localDateTimeFormatter.format(java.time.Instant.ofEpochMilli(this))
    } else {
        "Nunca sincronizado"
    }
}