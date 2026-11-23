package com.skywatch.cg.data.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarAlertRulesTest {
    private fun aircraft(altitude: Int = 4_500, rate: Int? = -500, distance: Double = 10.0, track: Double = 222.26, bearing: Double = 42.26, age: Double? = 10.0, category: String? = null, speed: Double = 140.0) =
        RadarAircraft("abc", "AZU4000", -20.4, -54.6, altitude, rate, speed, track, age, distance, bearing, emitterCategory = category)

    private fun trend(distanceTrend: RadarDistanceTrend) = RadarAircraftTrend(emptyList(), -1.0, null, -500, RadarVerticalTrend.DESCENDING, distanceTrend)

    @Test fun detectsArrival24ComingFromNortheast() {
        val alert = RadarAlertRules.evaluate(aircraft(), trend(RadarDistanceTrend.APPROACHING))
        assertEquals(RadarAlertType.ARRIVAL, alert?.type); assertEquals("24", alert?.runway); assertEquals("NE", alert?.lookDirection); assertEquals(4, alert?.estimatedMinutes)
    }

    @Test fun detectsArrival06ComingFromSouthwest() {
        val alert = RadarAlertRules.evaluate(aircraft(track = 42.26, bearing = 222.26), trend(RadarDistanceTrend.APPROACHING))
        assertEquals("06", alert?.runway); assertEquals("SO", alert?.lookDirection)
    }

    @Test fun rejectsDescendingAircraftOutsideRunwayAxis() = assertNull(RadarAlertRules.evaluate(aircraft(bearing = 0.0), trend(RadarDistanceTrend.APPROACHING)))

    @Test fun rejectsOverflightHelicopterOldPositionAndMissingData() {
        assertNull(RadarAlertRules.evaluate(aircraft(altitude = 12_000), trend(RadarDistanceTrend.APPROACHING)))
        assertNull(RadarAlertRules.evaluate(aircraft(category = "A7"), trend(RadarDistanceTrend.APPROACHING)))
        assertNull(RadarAlertRules.evaluate(aircraft(age = 61.0), trend(RadarDistanceTrend.APPROACHING)))
        assertNull(RadarAlertRules.evaluate(aircraft(rate = null), null))
    }

    @Test fun rejectsGoAroundMovingAwayFromAirport() = assertNull(RadarAlertRules.evaluate(aircraft(track = 222.26, bearing = 42.26), trend(RadarDistanceTrend.RECEDING)))

    @Test fun detectsDepartureOnRunwayAndRejectsClimbingFarFromAxis() {
        val departure = RadarAlertRules.evaluate(aircraft(altitude = 2_500, rate = 700, distance = 4.0, track = 42.26, bearing = 222.26), trend(RadarDistanceTrend.RECEDING))
        assertEquals(RadarAlertType.DEPARTURE, departure?.type); assertEquals("06", departure?.runway)
        assertNull(RadarAlertRules.evaluate(aircraft(altitude = 2_500, rate = 700, distance = 4.0, bearing = 90.0), trend(RadarDistanceTrend.RECEDING)))
    }

    @Test fun formatsDirectionAndDeduplicates() {
        assertEquals("N", directionForBearing(0.0)); assertEquals("L", directionForBearing(90.0)); assertEquals("SO", directionForBearing(225.0))
        var now = 0L; val dedup = RadarAlertDeduplicator { now }
        assertTrue(dedup.shouldAlert("abc", RadarAlertType.ARRIVAL)); assertTrue(!dedup.shouldAlert("abc", RadarAlertType.ARRIVAL))
        now += RadarAlertRules.ALERT_COOLDOWN_MILLIS; assertTrue(dedup.shouldAlert("abc", RadarAlertType.ARRIVAL))
    }

    @Test fun hysteresisKeepsOnlySmallReleaseMargin() {
        val previous = RadarAlert(RadarAlertType.ARRIVAL, 15.0, 2, 0.0)
        assertEquals(previous, RadarAlertHysteresis().retain(aircraft(distance = 16.5), null, previous))
        assertNull(RadarAlertHysteresis().retain(aircraft(distance = 18.0), null, previous))
    }
}
