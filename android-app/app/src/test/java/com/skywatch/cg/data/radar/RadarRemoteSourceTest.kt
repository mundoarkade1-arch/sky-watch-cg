package com.skywatch.cg.data.radar

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class RadarRemoteSourceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchesSnapshotFromConfiguredEndpoint() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"now":1793827327001,"ac":[{"hex":"abc123","lat":-20.4,"lon":-54.5}]}"""
            )
        )
        val source = RadarRemoteSource(server.url("/radar").toString(), OkHttpClient())

        val snapshot = source.fetchSnapshot()
        val request = server.takeRequest()

        assertEquals("/radar", request.path)
        assertEquals("SKYWATCH-CG/1.0 (experimental ADS-B radar)", request.getHeader("User-Agent"))
        assertEquals(1793827327001L, snapshot.observedAtMillis)
        assertEquals("abc123", snapshot.aircraft.single().icaoHex)
    }

    @Test
    fun reportsUnsuccessfulHttpResponse() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        val source = RadarRemoteSource(server.url("/radar").toString(), OkHttpClient())

        val error = assertThrows(IOException::class.java) {
            runBlocking { source.fetchSnapshot() }
        }

        assertEquals("Fonte ADS-B respondeu HTTP 503", error.message)
    }
}
