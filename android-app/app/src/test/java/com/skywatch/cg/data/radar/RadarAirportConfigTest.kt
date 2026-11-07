package com.skywatch.cg.data.radar

import org.junit.Assert.assertEquals
import org.junit.Test

class RadarAirportConfigTest {
    @Test
    fun sbcgUsesPublishedElevationAndRunwayHeadings() {
        assertEquals("SBCG", RadarAirportConfig.SBCG_ICAO)
        assertEquals(1_834, RadarAirportConfig.ELEVATION_FEET)
        assertEquals(559.0, RadarAirportConfig.ELEVATION_METERS, 0.0)
        assertEquals(42.26, RadarAirportConfig.RUNWAY_06_TRUE_HEADING_DEGREES, 0.0)
        assertEquals(222.26, RadarAirportConfig.RUNWAY_24_TRUE_HEADING_DEGREES, 0.0)
    }
}
