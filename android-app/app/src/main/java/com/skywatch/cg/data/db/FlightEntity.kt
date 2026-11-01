package com.skywatch.cg.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey
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
    val operationType: String,     // "ARRIVAL" ou "DEPARTURE"
    val scheduledDepartureUtc: String, // "HH:mm"
    val scheduledArrivalUtc: String,   // "HH:mm"
    val flightDate: String,        // "yyyy-MM-dd"
    val validFrom: String,         // "yyyy-MM-dd"
    val validTo: String,           // "yyyy-MM-dd"
    val serviceType: String?,
    val sirosStatus: String,       // Ex: "Em Operação", "A operar"
    
    // Campos operacionais e de controle arquitetural
    val operationalStatus: String = "UNKNOWN", // UNKNOWN, CONFIRMED, DELAYED, CANCELED
    val delayMinutes: Int? = null,             // Null quando sem confirmação
    val lastSyncAt: Long = System.currentTimeMillis(),
    val statusUpdatedAt: Long? = null
) {
    fun toNormalizedFlight(): NormalizedFlight {
        return NormalizedFlight(
            flightId = flightId,
            sirosRecordId = sirosRecordId,
            airline = airline,
            flightNumber = flightNumber,
            equipment = equipment,
            origin = origin,
            originName = originName,
            originTimezone = originTimezone,
            destination = destination,
            destinationName = destinationName,
            destinationTimezone = destinationTimezone,
            operationType = OperationType.valueOf(operationType),
            scheduledDepartureUtc = LocalTime.parse(scheduledDepartureUtc),
            scheduledArrivalUtc = LocalTime.parse(scheduledArrivalUtc),
            flightDate = LocalDate.parse(flightDate),
            daysOfWeek = emptyList(), // Persistido por data específica na tabela
            validFrom = LocalDate.parse(validFrom),
            validTo = LocalDate.parse(validTo),
            serviceType = serviceType,
            sirosStatus = sirosStatus
        )
    }

    companion object {
        fun fromNormalizedFlight(flight: NormalizedFlight): FlightEntity {
            return FlightEntity(
                flightId = flight.flightId,
                sirosRecordId = flight.sirosRecordId,
                airline = flight.airline,
                flightNumber = flight.flightNumber,
                equipment = flight.equipment,
                origin = flight.origin,
                originName = flight.originName,
                originTimezone = flight.originTimezone,
                destination = flight.destination,
                destinationName = flight.destinationName,
                destinationTimezone = flight.destinationTimezone,
                operationType = flight.operationType.name,
                scheduledDepartureUtc = flight.scheduledDepartureUtc.toString(),
                scheduledArrivalUtc = flight.scheduledArrivalUtc.toString(),
                flightDate = flight.flightDate.toString(),
                validFrom = flight.validFrom.toString(),
                validTo = flight.validTo.toString(),
                serviceType = flight.serviceType,
                sirosStatus = flight.sirosStatus
            )
        }
    }
}
