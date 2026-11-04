package com.skywatch.cg.data.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RadarSnapshotParserTest {
    @Test
    fun parsesPositionAndOmitsUnreportedAircraftIdentity() {
        val snapshot = RadarSnapshotParser.parse(
            """
                {
                  "now": 1793827327001,
                  "ac": [{
                    "hex": "ABC123",
                    "flight": " TEST123 ",
                    "lat": -20.802658,
                    "lon": -55.400129,
                    "alt_baro": 38375,
                    "gs": 293.5,
                    "track": 128.08,
                    "seen_pos": 0.4,
                    "dst": 45.472,
                    "dir": 243.7
                  }, {
                    "hex": "def456",
                    "lat": -20.5,
                    "lon": -54.7,
                    "alt_baro": "ground"
                  }]
                }
            """.trimIndent(),
            RadarRemoteSource.CENTER_LATITUDE,
            RadarRemoteSource.CENTER_LONGITUDE
        )

        assertEquals(1793827327001L, snapshot.observedAtMillis)
        assertEquals(2, snapshot.aircraft.size)
        assertEquals("abc123", snapshot.aircraft[0].icaoHex)
        assertEquals("TEST123", snapshot.aircraft[0].callsign)
        assertEquals(38375, snapshot.aircraft[0].altitudeFeet)
        assertEquals(45.472, snapshot.aircraft[0].distanceNauticalMiles, 0.001)
        assertEquals(243.7, snapshot.aircraft[0].bearingDegrees, 0.001)
        assertNull(snapshot.aircraft[1].callsign)
        assertNull(snapshot.aircraft[1].altitudeFeet)
    }

    @Test
    fun filtersEntriesWithoutValidPosition() {
        val snapshot = RadarSnapshotParser.parse(
            """
                {
                  "now": 1793827327001,
                  "ac": [
                    {"hex":"abc123","lat":-20.4,"lon":-54.5},
                    {"hex":"def456","lat":null,"lon":-54.5},
                    {"hex":"ghi789","lat":120,"lon":-54.5},
                    {"hex":"   ","lat":-20.4,"lon":-54.5}
                  ]
                }
            """.trimIndent(),
            RadarRemoteSource.CENTER_LATITUDE,
            RadarRemoteSource.CENTER_LONGITUDE
        )

        assertEquals(1, snapshot.aircraft.size)
        assertEquals("abc123", snapshot.aircraft.single().icaoHex)
    }

    @Test
    fun acceptsValidEmptySnapshot() {
        val snapshot = RadarSnapshotParser.parse(
            """{"now":1793827327001,"ac":[]}""",
            RadarRemoteSource.CENTER_LATITUDE,
            RadarRemoteSource.CENTER_LONGITUDE
        )

        assertTrue(snapshot.aircraft.isEmpty())
    }

    @Test(expected = IOException::class)
    fun rejectsResponseWithoutTimestamp() {
        RadarSnapshotParser.parse(
            """{"ac":[]}""",
            RadarRemoteSource.CENTER_LATITUDE,
            RadarRemoteSource.CENTER_LONGITUDE
        )
    }
}
