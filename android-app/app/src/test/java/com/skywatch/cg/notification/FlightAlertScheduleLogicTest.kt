package com.skywatch.cg.notification

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FlightAlertScheduleLogicTest {
    @Test fun doesNotScheduleWhenAlertsAreDisabled() {
        assertEquals(false, FlightAlertScheduler.shouldScheduleAlerts(false))
        assertEquals(true, FlightAlertScheduler.shouldScheduleAlerts(true))
    }

    @Test fun exactAlarmPolicyUsesExactBeforeAndroid12OrWhenAllowed() {
        assertEquals(true, FlightAlertScheduler.shouldUseExactAlarm(30, false))
        assertEquals(true, FlightAlertScheduler.shouldUseExactAlarm(31, true))
        assertEquals(false, FlightAlertScheduler.shouldUseExactAlarm(31, false))
    }

    @Test fun schedulesAtEventMinusSelectedMinutes() {
        val plan = FlightAlertScheduleLogic.plan("siros-2026-11-05", 1_000_000L, 15)
        assertEquals(100_000L, plan.triggerAtMillis)
        assertEquals("siros-2026-11-05:15", plan.alertId)
    }

    @Test fun neverUsesNegativeMinutes() {
        val plan = FlightAlertScheduleLogic.plan("flight", 1_000L, -5)
        assertEquals(1_000L, plan.triggerAtMillis)
    }

    @Test fun convertsStoredUtcEventToTheCorrectInstant() {
        val flight = NormalizedFlight(
            flightId = "flight-1",
            sirosRecordId = "siros-1",
            airline = "Teste",
            flightNumber = "1234",
            equipment = null,
            origin = "SBCG",
            originName = null,
            originTimezone = "America/Campo_Grande",
            destination = "SBGR",
            destinationName = null,
            destinationTimezone = "America/Sao_Paulo",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(12, 0),
            scheduledArrivalUtc = LocalTime.of(14, 0),
            flightDate = LocalDate.of(2026, 11, 6),
            arrivalDate = LocalDate.of(2026, 11, 6),
            daysOfWeek = emptyList(),
            validFrom = LocalDate.of(2026, 11, 1),
            validTo = LocalDate.of(2026, 11, 30),
            serviceType = "PASSENGER",
            sirosStatus = "A operar"
        )

        assertEquals(1793966400000L, FlightAlertScheduler.eventAtMillis(flight))
    }
}
