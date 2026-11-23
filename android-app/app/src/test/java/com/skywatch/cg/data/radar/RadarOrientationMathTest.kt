package com.skywatch.cg.data.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarOrientationMathTest {
    @Test fun normalizesNorthAndEast() {
        assertEquals(0f, normalizeAzimuthDegrees(0f), 0.001f)
        assertEquals(90f, normalizeAzimuthDegrees(90f), 0.001f)
    }

    @Test fun crossesZeroUsingShortestPath() {
        assertEquals(2f, shortestAzimuthDeltaDegrees(359f, 1f), 0.001f)
        assertEquals(0f, normalizeAzimuthDegrees(smoothAzimuthDegrees(359f, 1f, 0.5f)), 0.001f)
    }

    @Test fun smoothsWithoutJumping() {
        assertEquals(10f, smoothAzimuthDegrees(0f, 50f, 0.2f), 0.001f)
        assertTrue(smoothAzimuthDegrees(null, 370f) == 10f)
    }

    @Test fun absentSensorIsSupported() {
        assertEquals(false, hasRotationVectorSensor(false))
        assertEquals(true, hasRotationVectorSensor(true))
    }
}
