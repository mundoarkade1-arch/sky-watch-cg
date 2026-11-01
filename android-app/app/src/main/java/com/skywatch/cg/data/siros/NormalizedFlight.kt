package com.skywatch.cg.data.siros

import java.time.LocalDate
import java.time.LocalTime

enum class OperationType {
    ARRIVAL, DEPARTURE
}

/**
 * Modelo de voo normalizado pronto para uso no aplicativo e persistência em cache.
 * Respeita a separação de fontes (planejamento SIROS) e o tratamento correto de fusos horários.
 */
data class NormalizedFlight(
    val flightId: String,          // Chave única composta: sirosRecordId + data específica do voo
    val sirosRecordId: String,     // Identificador original SIROS para deduplicação
    val airline: String,
    val flightNumber: String,
    val equipment: String?,
    val origin: String,
    val originName: String?,
    val originTimezone: String,    // Ex: "America/Sao_Paulo"
    val destination: String,
    val destinationName: String?,
    val destinationTimezone: String, // Ex: "America/Campo_Grande"
    val operationType: OperationType,
    val scheduledDepartureUtc: LocalTime,
    val scheduledArrivalUtc: LocalTime,
    val flightDate: LocalDate,     // Data específica da operação
    val daysOfWeek: List<Int>,     // Dias da semana em que opera (1=Segunda, ..., 7=Domingo)
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val serviceType: String?,
    val sirosStatus: String        // Ex: "Em Operação", "A operar"
)
