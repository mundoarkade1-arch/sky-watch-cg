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

/**
 * Formata o nome da companhia aérea removendo sufixos comuns.
 * Exemplo: "AZUL LINHAS AÉREAS BRASILEIRAS S/A" -> "AZUL"
 * Exemplo: "GOL LINHAS AEREAS" -> "GOL"
 * Exemplo: "LATAM AIRLINES" -> "LATAM"
 */
internal fun formatAirlineName(airline: String): String {
    return airline
        .replace("LINHAS AÉREAS", "", ignoreCase = true)
        .replace("LINHAS AEREAS", "", ignoreCase = true)
        .replace("BRASILEIRAS", "", ignoreCase = true)
        .replace("S.A.", "", ignoreCase = true)
        .replace("S/A", "", ignoreCase = true)
        .replace("AIRLINES", "", ignoreCase = true)
        .trim()
        .let { if (it.isEmpty()) airline else it }
}

/**
 * Formata o nome do aeroporto para exibição: "Cidade (CÓDIGO)".
 * Remove "BRASIL" e estado (ex: "- SP").
 * Exemplo: "VIRACOPOS - CAMPINAS - SP - BRASIL (SBKP)" -> "Campinas (SBKP)"
 * Exemplo: "GUARULHOS (SBGR)" -> "Guarulhos (SBGR)"
 * Se não conseguir separar a cidade com segurança, retorna o nome sem sufixos.
 */
internal fun formatAirportDisplay(name: String?, code: String): String {
    val rawName = name ?: code
    // Remove "BRASIL" e o que vier depois (estado, etc.)
    val withoutBrasil = rawName.split("BRASIL", ignoreCase = true).first().trim()
    // Remove sufixo de estado tipo " - SP", " - RJ", etc.
    val withoutState = withoutBrasil.replace(Regex("\\s*-\\s*[A-Z]{2}\\s*$"), "").trim()
    // Remove trailing separators
    val cleanName = withoutState.trimEnd('-').trim()
    // Tenta capitalizar a primeira letra de cada palavra (caso venha tudo maiúsculo)
    val displayName = cleanName.split(" ").map { it.lowercase().replaceFirstChar { it.uppercase() } }.joinToString(" ")
    return if (displayName.equals(code, ignoreCase = true)) {
        code
    } else {
        "$displayName ($code)"
    }
}

/**
 * Formata o tempo até o voo: "em Xh Ym" ou "em X min".
 */
internal fun formatTimeUntil(minutes: Long): String {
    return if (minutes <= 0) {
        "agora"
    } else if (minutes < 60) {
        "em ${minutes} min"
    } else {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins == 0L) "em ${hours}h" else "em ${hours}h ${mins}m"
    }
}