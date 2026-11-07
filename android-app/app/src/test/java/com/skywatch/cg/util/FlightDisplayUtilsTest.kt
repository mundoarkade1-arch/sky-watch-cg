package com.skywatch.cg.util

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FlightDisplayUtilsTest {

    @Test
    fun testFormatAirportDisplayViracopos() {
        val name = "VIRACOPOS - CAMPINAS - SP - BRASIL (SBKP)"
        val code = "SBKP"
        val result = formatAirportDisplay(name, code)
        assertEquals("Campinas (SBKP)", result)
    }

    @Test
    fun testFormatAirportDisplayGuarulhos() {
        val name = "GUARULHOS - GOVERNADOR ANDRÉ FRANCO MONTORO - GUARULHOS - SP - BRASIL (SBGR)"
        val code = "SBGR"
        val result = formatAirportDisplay(name, code)
        assertEquals("Guarulhos (SBGR)", result)
    }

    @Test
    fun testFormatAirportDisplayNonBrazilian() {
        val name = "JOHN F KENNEDY INTERNATIONAL AIRPORT (KJFK)"
        val code = "KJFK"
        val result = formatAirportDisplay(name, code)
        assertEquals("John F Kennedy International Airport (KJFK)", result)
    }

    @Test
    fun testFormatAirportDisplaySimpleName() {
        val name = "GUARULHOS"
        val code = "SBGR"
        val result = formatAirportDisplay(name, code)
        assertEquals("Guarulhos (SBGR)", result)
    }

    @Test
    fun testFormatAirportDisplayNullName() {
        val result = formatAirportDisplay(null, "SBGR")
        assertEquals("SBGR", result)
    }

    @Test
    fun testFormatAirportDisplayNameEqualsCode() {
        val name = "SBGR"
        val code = "SBGR"
        val result = formatAirportDisplay(name, code)
        assertEquals("SBGR", result)
    }

    @Test
    fun formatAirportDisplayKeepsUnknownPatternWithoutInventingCity() {
        val result = formatAirportDisplay("AEROPORTO REMOTO", "ZZZZ")

        assertEquals("Aeroporto Remoto (ZZZZ)", result)
    }

    @Test
    fun formatAirlineNameRemovesPortugueseAndCorporateSuffixes() {
        assertEquals("AZUL", formatAirlineName("AZUL LINHAS AÉREAS BRASILEIRAS S/A"))
        assertEquals("LATAM", formatAirlineName("LATAM AIRLINES"))
        assertEquals("GOL", formatAirlineName("GOL (EX-VRG) LINHAS AEREAS"))
    }

    @Test
    fun arrivalAndDepartureDisplayTheAirportLocalTime() {
        val date = LocalDate.of(2026, 11, 4)
        val arrival = flight(
            id = "arrival",
            operationType = OperationType.ARRIVAL,
            flightDate = date,
            arrivalDate = date,
            scheduledDepartureUtc = LocalTime.of(8, 0),
            scheduledArrivalUtc = LocalTime.of(9, 30)
        )
        val departure = flight(
            id = "departure",
            operationType = OperationType.DEPARTURE,
            flightDate = date,
            arrivalDate = date,
            scheduledDepartureUtc = LocalTime.of(10, 0),
            scheduledArrivalUtc = LocalTime.of(11, 30)
        )

        assertEquals("05:30", arrival.getDisplayTime())
        assertEquals("06:00", departure.getDisplayTime())
    }

    @Test
    fun overnightArrivalUsesTheArrivalUtcDateBeforeConvertingToCampoGrande() {
        val flightDate = LocalDate.of(2026, 11, 4)
        val flight = flight(
            id = "overnight-arrival",
            operationType = OperationType.ARRIVAL,
            flightDate = flightDate,
            arrivalDate = flightDate.plusDays(1),
            scheduledDepartureUtc = LocalTime.of(22, 0),
            scheduledArrivalUtc = LocalTime.of(1, 0)
        )

        assertEquals("04/11/2026 21:00", flight.getDisplayDateTime())
    }

    private fun flight(
        id: String,
        operationType: OperationType,
        flightDate: LocalDate,
        arrivalDate: LocalDate,
        scheduledDepartureUtc: LocalTime,
        scheduledArrivalUtc: LocalTime
    ) = NormalizedFlight(
        flightId = id,
        sirosRecordId = id,
        airline = "Teste",
        flightNumber = "1234",
        equipment = null,
        origin = if (operationType == OperationType.ARRIVAL) "SBGR" else "SBCG",
        originName = null,
        originTimezone = if (operationType == OperationType.ARRIVAL) "America/Sao_Paulo" else "America/Campo_Grande",
        destination = if (operationType == OperationType.ARRIVAL) "SBCG" else "SBGR",
        destinationName = null,
        destinationTimezone = if (operationType == OperationType.ARRIVAL) "America/Campo_Grande" else "America/Sao_Paulo",
        operationType = operationType,
        scheduledDepartureUtc = scheduledDepartureUtc,
        scheduledArrivalUtc = scheduledArrivalUtc,
        flightDate = flightDate,
        arrivalDate = arrivalDate,
        daysOfWeek = emptyList(),
        validFrom = flightDate,
        validTo = flightDate,
        serviceType = "REGULAR DE PASSAGEIROS",
        sirosStatus = "Em Operação"
    )
}
