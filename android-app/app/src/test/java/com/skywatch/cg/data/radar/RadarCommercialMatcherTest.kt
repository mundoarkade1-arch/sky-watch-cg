package com.skywatch.cg.data.radar

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RadarCommercialMatcherTest {
    private val now = Instant.parse("2026-11-22T14:00:00Z")
    private fun flight(time: LocalTime = LocalTime.of(14, 30)) = NormalizedFlight(
        flightId = "id", sirosRecordId = "id", airline = "AZUL", flightNumber = "4000",
        equipment = null, origin = "SBGR", originName = null, originTimezone = "UTC",
        destination = "SBCG", destinationName = null, destinationTimezone = "UTC",
        operationType = OperationType.ARRIVAL, scheduledDepartureUtc = time,
        scheduledArrivalUtc = time, flightDate = LocalDate.of(2026, 11, 22),
        arrivalDate = LocalDate.of(2026, 11, 22), daysOfWeek = listOf(7),
        validFrom = LocalDate.of(2026, 1, 1), validTo = LocalDate.of(2026, 12, 31),
        serviceType = "REGULAR DE PASSAGEIROS", sirosStatus = "A OPERAR"
    )

    @Test fun matchesKnownCompanyInsideWindow() {
        val result = RadarCommercialMatcher.match("AZU4000", now, listOf(flight()))
        assertEquals(CommercialMatchStatus.MATCHED, result.status)
        assertNotNull(result.flight)
    }

    @Test fun rejectsOutsideWindowAndUnknownCallsign() {
        assertEquals(CommercialMatchStatus.NOT_MATCHED, RadarCommercialMatcher.match("AZU4000", now, listOf(flight(LocalTime.of(18, 0)))).status)
        assertEquals(CommercialMatchStatus.UNKNOWN, RadarCommercialMatcher.match("N123AB", now, listOf(flight())).status)
    }
}
