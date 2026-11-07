package com.skywatch.cg.util

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NextMovementsTest {
    private fun flight(id: String, type: OperationType, time: LocalTime) = NormalizedFlight(
        flightId = id, sirosRecordId = id, airline = "TESTE", flightNumber = id,
        equipment = null, origin = "SBSP", originName = "São Paulo", originTimezone = "America/Sao_Paulo",
        destination = "SBCG", destinationName = "Campo Grande", destinationTimezone = "America/Campo_Grande",
        operationType = type, scheduledDepartureUtc = time.plusHours(4), scheduledArrivalUtc = time.plusHours(4),
        flightDate = LocalDate.of(2026, 11, 5), arrivalDate = LocalDate.of(2026, 11, 5),
        daysOfWeek = emptyList(), validFrom = LocalDate.of(2026, 1, 1), validTo = LocalDate.of(2026, 12, 31),
        serviceType = null, sirosStatus = "A operar"
    )

    @Test fun choosesNextArrivalAndDepartureAndHandlesNoRemaining() {
        val result = nextMovements(
            listOf(flight("A", OperationType.ARRIVAL, LocalTime.of(10, 0)), flight("D", OperationType.DEPARTURE, LocalTime.of(11, 0))),
            LocalDateTime.of(2026, 11, 5, 10, 30)
        )
        assertNull(result.nextArrival)
        assertEquals("D", result.nextDeparture?.flightId)
    }

    @Test fun ignoresMovementsFromEarlierDates() {
        val yesterday = flight("Y", OperationType.ARRIVAL, LocalTime.of(10, 0)).copy(
            flightDate = LocalDate.of(2026, 11, 4),
            arrivalDate = LocalDate.of(2026, 11, 4)
        )

        val result = nextMovements(
            listOf(yesterday),
            LocalDateTime.of(2026, 11, 5, 8, 0)
        )

        assertNull(result.nextArrival)
        assertNull(result.nextDeparture)
    }

    @Test fun todayMovementAlreadyPastIsExcluded() {
        val result = nextMovements(
            listOf(flight("PAST", OperationType.ARRIVAL, LocalTime.of(10, 0))),
            LocalDateTime.of(2026, 11, 5, 10, 30)
        )

        assertNull(result.nextArrival)
    }

    @Test fun futureDateMovementIsIncluded() {
        val tomorrow = flight("FUTURE", OperationType.ARRIVAL, LocalTime.of(10, 0)).copy(
            flightDate = LocalDate.of(2026, 11, 6),
            arrivalDate = LocalDate.of(2026, 11, 6)
        )

        val result = nextMovements(
            listOf(tomorrow),
            LocalDateTime.of(2026, 11, 5, 23, 59)
        )

        assertEquals("FUTURE", result.nextArrival?.flightId)
    }
}
