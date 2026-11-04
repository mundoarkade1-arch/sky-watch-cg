package com.skywatch.cg.util

import org.junit.Assert.*
import org.junit.Test

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
}