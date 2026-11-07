package com.skywatch.cg.data.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarTrajectoryRulesTest {
    private fun aircraft(
        hex: String = "abc123",
        altitude: Int = 8_000,
        baroRate: Int? = -500,
        geometricRate: Int? = null,
        speed: Double? = 140.0,
        distance: Double = 12.0,
        secondsSincePosition: Double? = 0.0,
        category: String? = null
    ) = RadarAircraft(
        icaoHex = hex,
        callsign = "TEST",
        latitude = -20.45,
        longitude = -54.60,
        altitudeFeet = altitude,
        barometricRateFeetPerMinute = baroRate,
        groundSpeedKnots = speed,
        trackDegrees = 180.0,
        secondsSincePosition = secondsSincePosition,
        distanceNauticalMiles = distance,
        bearingDegrees = 0.0,
        geometricRateFeetPerMinute = geometricRate,
        emitterCategory = category
    )

    @Test
    fun typicalLandingUsesAltitudeAboveAirport() {
        assertEquals(RadarAlertType.ARRIVAL, RadarAlertRules.evaluate(aircraft())?.type)
    }

    @Test
    fun typicalTakeoffUsesAltitudeAboveAirport() {
        assertEquals(
            RadarAlertType.DEPARTURE,
            RadarAlertRules.evaluate(aircraft(altitude = 4_000, baroRate = 700, distance = 5.0))?.type
        )
    }

    @Test
    fun highOverflightIsIgnoredUsingAgl() {
        assertNull(RadarAlertRules.evaluate(aircraft(altitude = RadarAirportConfig.ELEVATION_FEET + RadarAlertRules.HIGH_PASSING_AGL_FEET + 1)))
    }

    @Test
    fun stationaryAircraftIsIgnored() {
        assertNull(RadarAlertRules.evaluate(aircraft(speed = 0.0)))
    }

    @Test
    fun stalePositionIsIgnored() {
        assertNull(RadarAlertRules.evaluate(aircraft(secondsSincePosition = RadarAlertRules.MAX_POSITION_AGE_SECONDS + 1)))
    }

    @Test
    fun invalidPositionIsIgnored() {
        assertNull(RadarAlertRules.evaluate(aircraft().copy(latitude = 91.0)))
    }

    @Test
    fun missingBarometricRateUsesGeometricRateWhenPresent() {
        val alert = RadarAlertRules.evaluate(aircraft(baroRate = null, geometricRate = -500))
        assertEquals(RadarAlertType.ARRIVAL, alert?.type)
    }

    @Test
    fun missingRatesUseAltitudeSamplesToDeriveDescent() {
        val history = RadarAircraftHistory()
        history.observe(aircraft(baroRate = null, altitude = 8_000, distance = 15.0), 1_000L)
        val trend = history.observe(aircraft(baroRate = null, altitude = 7_500, distance = 12.0), 61_000L)

        assertEquals(RadarVerticalTrend.DESCENDING, trend.verticalTrend)
        assertTrue(trend.distanceTrend == RadarDistanceTrend.APPROACHING)
        assertEquals(RadarAlertType.ARRIVAL, RadarAlertRules.evaluate(aircraft(baroRate = null), trend)?.type)
    }

    @Test
    fun helicopterIsIgnoredConservativelyWhenCategoryIsA7() {
        assertNull(RadarAlertRules.evaluate(aircraft(category = RadarAlertRules.HELICOPTER_EMITTER_CATEGORY)))
    }

    @Test
    fun historyIsShortAndKeyedByHex() {
        val history = RadarAircraftHistory(maxSamples = 2)
        history.observe(aircraft(hex = "one"), 1_000L)
        history.observe(aircraft(hex = "one", distance = 11.0), 2_000L)
        history.observe(aircraft(hex = "one", distance = 10.0), 3_000L)
        history.observe(aircraft(hex = "two"), 3_000L)

        assertEquals(2, history.samplesFor("one").size)
        assertEquals(1, history.samplesFor("two").size)
    }
}
