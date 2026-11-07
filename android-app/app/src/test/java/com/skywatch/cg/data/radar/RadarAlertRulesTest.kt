package com.skywatch.cg.data.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarAlertRulesTest {
    private fun aircraft(
        altitude: Int,
        rate: Int,
        distance: Double,
        track: Double = 180.0,
        bearing: Double = 0.0
    ) = RadarAircraft("abc", null, -20.4, -54.6, altitude, rate, 456.0, track, 0.0, distance, bearing)

    @Test fun detectsArrivalAndEstimatesTime() {
        val alert = RadarAlertRules.evaluate(aircraft(8_000, -500, 12.0))
        assertEquals(RadarAlertType.ARRIVAL, alert?.type)
        assertEquals(1, alert?.estimatedMinutes)
    }

    @Test fun detectsDeparture() {
        val alert = RadarAlertRules.evaluate(aircraft(4_000, 700, 5.0))
        assertEquals(RadarAlertType.DEPARTURE, alert?.type)
    }

    @Test fun rejectsHighPassingAircraft() {
        assertNull(RadarAlertRules.evaluate(aircraft(34_475, -500, 47.0, 152.0, 0.0)))
    }

    @Test fun deduplicatesByAircraftAndTypeForTwentyMinutes() {
        var now = 0L
        val dedup = RadarAlertDeduplicator { now }
        assertTrue(dedup.shouldAlert("abc", RadarAlertType.ARRIVAL))
        assertTrue(!dedup.shouldAlert("abc", RadarAlertType.ARRIVAL))
        now += RadarAlertRules.ALERT_COOLDOWN_MILLIS
        assertTrue(dedup.shouldAlert("abc", RadarAlertType.ARRIVAL))
        assertTrue(dedup.shouldAlert("abc", RadarAlertType.DEPARTURE))
    }

    @Test fun formatsDirectionForLookingTowardAircraft() {
        assertEquals("N", directionForBearing(0.0))
        assertEquals("L", directionForBearing(90.0))
        assertEquals("SO", directionForBearing(225.0))
    }

    @Test fun hysteresisKeepsAlertInsideReleaseMargin() {
        val previous = RadarAlert(RadarAlertType.ARRIVAL, 25.0, 2, 0.0)
        val aircraft = aircraft(9_500, -100, 26.0)

        assertEquals(previous, RadarAlertHysteresis().retain(aircraft, null, previous))
    }

    @Test fun hysteresisReleasesAlertOutsideReleaseMargin() {
        val previous = RadarAlert(RadarAlertType.ARRIVAL, 25.0, 2, 0.0)
        val aircraft = aircraft(9_500, -100, 28.0)

        assertNull(RadarAlertHysteresis().retain(aircraft, null, previous))
    }

    @Test fun headlineShowsAdditionalAlertCount() {
        assertEquals("VOO DETECTADO", radarAlertHeadline(1))
        assertEquals("VOO DETECTADO · +2", radarAlertHeadline(3))
    }
}
