package com.skywatch.cg.util

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.Normalizer
import java.util.Locale

/**
 * Funções de utilidade para formatação de voos.
 */
internal val campoGrandeZone = ZoneId.of("America/Campo_Grande")
internal val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Campo_Grande"))

private val airportTimezonesByIcao = mapOf(
    "SBCG" to "America/Campo_Grande",
    "SBGR" to "America/Sao_Paulo",
    "SBSP" to "America/Sao_Paulo",
    "SBKP" to "America/Sao_Paulo",
    "SBBR" to "America/Sao_Paulo",
    "KJFK" to "America/New_York",
    "EGLL" to "Europe/London"
)

internal fun airportTimezoneForIcao(icao: String, fallback: String): String =
    airportTimezonesByIcao[icao.trim().uppercase(Locale.ROOT)] ?: fallback

/**
 * Retorna o horário local formatado (HH:mm) para chegada ou partida do voo.
 */
internal fun NormalizedFlight.getAirportEventDateTime(): LocalDateTime {
    val isArrival = operationType == OperationType.ARRIVAL
    val utcTime = if (isArrival) scheduledArrivalUtc else scheduledDepartureUtc
    val utcDate = if (isArrival) arrivalDate else flightDate
    val airportIcao = if (isArrival) destination else origin
    val declaredTimezone = if (isArrival) destinationTimezone else originTimezone
    val airportTimezone = airportTimezoneForIcao(airportIcao, declaredTimezone)
    return utcDate.atTime(utcTime)
        .atZone(ZoneId.of("UTC"))
        .withZoneSameInstant(ZoneId.of(airportTimezone))
        .toLocalDateTime()
}

internal fun NormalizedFlight.getDisplayTime(): String =
    getAirportEventDateTime().format(timeFormatter)

internal fun NormalizedFlight.getDisplayDate(): String =
    getAirportEventDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

internal fun NormalizedFlight.getDisplayDateTime(): String =
    getAirportEventDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

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
    val normalized = Normalizer.normalize(airline, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .uppercase(Locale.ROOT)
    val cleaned = normalized
        .replace(Regex("\\s*\\(EX-\\s*VRG\\s*\\)"), "")
        .replace("LINHAS AEREAS", "")
        .replace("BRASILEIRAS", "")
        .replace("S.A.", "")
        .replace("S/A", "")
        .replace("AIRLINES", "")
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (cleaned.isEmpty()) airline else cleaned
}

/**
 * Formata o nome do aeroporto para exibição: "Cidade (CÓDIGO)".
 * Para aeroportos brasileiros no formato "NOME1 - CIDADE - UF - BRASIL (CÓDIGO)":
 * - Separa por " - "
 * - Remove "BRASIL" e a sigla de 2 letras do estado (SP, MS, RJ, etc.)
 * - A cidade é o ÚLTIMO pedaço que sobrar
 * Exemplo: "VIRACOPOS - CAMPINAS - SP - BRASIL (SBKP)" -> "Campinas (SBKP)"
 * Exemplo: "GUARULHOS - GOVERNADOR ANDRÉ FRANCO MONTORO - GUARULHOS - SP - BRASIL (SBGR)" -> "Guarulhos (SBGR)"
 * Se não seguir o padrão brasileiro, retorna o nome original sem inventar.
 */
internal fun formatAirportDisplay(name: String?, code: String): String {
    val rawName = name ?: return code
    
    // Extrai o código ICAO se estiver entre parênteses no final
    val icaoInParens = Regex("\\(([A-Z]{4})\\)$").find(rawName)?.groupValues?.get(1)
    val effectiveCode = icaoInParens ?: code
    
    // Remove o código ICAO do final se estiver lá
    val nameWithoutIcao = rawName.replace(Regex("\\s*\\([A-Z]{4}\\)\\s*$"), "").trim()
    
    // Separa por " - "
    val parts = nameWithoutIcao.split(" - ").map { it.trim() }.filter { it.isNotEmpty() }
    
    // Se não tem separadores ou tem poucos pedaços, não é padrão brasileiro
    if (parts.size < 3) {
        // Tenta usar o nome original (sem o código ICAO se tinha)
        val displayName = nameWithoutIcao
        return if (displayName.equals(effectiveCode, ignoreCase = true)) {
            effectiveCode
        } else {
            "${capitalizeWords(displayName)} ($effectiveCode)"
        }
    }
    
    // Remove "BRASIL" (case-insensitive) e siglas de estado de 2 letras
    val brazilianStates = setOf("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", 
                                "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", 
                                "RS", "RO", "RR", "SC", "SP", "SE", "TO")
    
    val filteredParts = parts.filter { part ->
        val upper = part.uppercase()
        !upper.equals("BRASIL") && !brazilianStates.contains(upper)
    }
    
    // A cidade é o ÚLTIMO pedaço que sobrar
    val city = filteredParts.lastOrNull() ?: parts.last()
    
    val displayName = capitalizeWords(city)
    return if (displayName.equals(effectiveCode, ignoreCase = true)) {
        effectiveCode
    } else {
        "$displayName ($effectiveCode)"
    }
}

/**
 * Capitaliza a primeira letra de cada palavra, resto minúsculo.
 */
private fun capitalizeWords(text: String): String {
    return text.split(" ").map { word ->
        if (word.isEmpty()) word else word.lowercase().replaceFirstChar { it.uppercase() }
    }.joinToString(" ")
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
