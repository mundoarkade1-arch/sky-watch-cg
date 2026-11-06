package com.skywatch.cg.util

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ObservationWindowTest {

    private val date = LocalDate.of(2026, 11, 5)

    @Test
    fun findsTheThreeHourWindowWithMostArrivalsAndDepartures() {
        val flights = listOf(
            flight("a1", OperationType.ARRIVAL, "08:15"),
            flight("d1", OperationType.DEPARTURE, "09:00"),
            flight("a2", OperationType.ARRIVAL, "10:30"),
            flight("d2", OperationType.DEPARTURE, "12:00"),
            flight("d3", OperationType.DEPARTURE, "15:00"),
            flight("a3", OperationType.ARRIVAL, "16:00"),
            flight("a4", OperationType.ARRIVAL, "16:30")
        )

        val result = ObservationWindowCalculator.findBestWindow(
            flights = flights,
            selectedDate = date,
            now = date.minusDays(1).atTime(12, 0)
        )

        assertEquals(date.atTime(8, 15), result?.start)
        assertEquals(date.atTime(11, 15), result?.end)
        assertEquals(2, result?.arrivals)
        assertEquals(1, result?.departures)
        assertEquals(3, result?.totalMovements)
    }

    @Test
    fun todayRecommendationOnlyCountsEventsThatHaveNotPassed() {
        val flights = listOf(
            flight("past-arrival", OperationType.ARRIVAL, "08:15"),
            flight("past-departure", OperationType.DEPARTURE, "09:00"),
            flight("future-arrival", OperationType.ARRIVAL, "10:30"),
            flight("future-departure", OperationType.DEPARTURE, "12:00")
        )

        val result = ObservationWindowCalculator.findBestWindow(
            flights = flights,
            selectedDate = date,
            now = date.atTime(9, 30)
        )

        assertEquals(date.atTime(10, 30), result?.start)
        assertEquals(1, result?.arrivals)
        assertEquals(1, result?.departures)
    }

    @Test
    fun doesNotRecommendAWindowThatExtendsBeyondTheSelectedDay() {
        val flights = listOf(
            flight("late", OperationType.ARRIVAL, "23:30")
        )

        val result = ObservationWindowCalculator.findBestWindow(
            flights = flights,
            selectedDate = date,
            now = date.minusDays(1).atTime(12, 0)
        )

        assertNull(result)
    }

    @Test
    fun allowsAWindowThatEndsExactlyAtMidnight() {
        val flights = listOf(
            flight("late", OperationType.ARRIVAL, "21:00")
        )

        val result = ObservationWindowCalculator.findBestWindow(
            flights = flights,
            selectedDate = date,
            now = date.minusDays(1).atTime(12, 0)
        )

        assertEquals(date.atTime(21, 0), result?.start)
        assertEquals(date.plusDays(1).atStartOfDay(), result?.end)
        assertEquals(1, result?.arrivals)
    }

    private fun flight(
        id: String,
        operationType: OperationType,
        localTime: String
    ): NormalizedFlight {
        val time = LocalTime.parse(localTime)
        val utcTime = time.plusHours(4)
        val utcDate = if (utcTime.isBefore(time)) date.plusDays(1) else date
        return NormalizedFlight(
            flightId = id,
            sirosRecordId = id,
            airline = "Teste",
            flightNumber = "1234",
            equipment = null,
            origin = if (operationType == OperationType.ARRIVAL) "SBGR" else "SBCG",
            originName = null,
            destination = if (operationType == OperationType.ARRIVAL) "SBCG" else "SBGR",
            destinationName = null,
            originTimezone = if (operationType == OperationType.ARRIVAL) "America/Sao_Paulo" else "America/Campo_Grande",
            destinationTimezone = if (operationType == OperationType.ARRIVAL) "America/Campo_Grande" else "America/Sao_Paulo",
            operationType = operationType,
            scheduledDepartureUtc = if (operationType == OperationType.DEPARTURE) utcTime else utcTime.minusHours(1),
            scheduledArrivalUtc = if (operationType == OperationType.ARRIVAL) utcTime else utcTime.plusHours(1),
            flightDate = if (operationType == OperationType.DEPARTURE) utcDate else date,
            arrivalDate = if (operationType == OperationType.ARRIVAL) utcDate else date,
            daysOfWeek = emptyList(),
            validFrom = date,
            validTo = date,
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
    }
}
