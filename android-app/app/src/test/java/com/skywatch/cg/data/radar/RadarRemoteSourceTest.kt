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
        assertEquals("SkyWatchCG/1.0 (app pessoal)", request.getHeader("User-Agent"))
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

        assertEquals("Fontes indisponíveis: teste: Fonte ADS-B respondeu HTTP 503", error.message)
    }

    @Test
    fun failsOverAfterRateLimit() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setBody("""{"now":1793827327001,"ac":[]}"""))
        val source = RadarRemoteSource(
            endpoints = listOf(
                RadarEndpoint("primeira", server.url("/one").toString()),
                RadarEndpoint("segunda", server.url("/two").toString())
            ),
            client = OkHttpClient()
        )

        val snapshot = source.fetchSnapshot()

        assertEquals("segunda", snapshot.sourceName)
        assertEquals("/one", server.takeRequest().path)
        assertEquals("/two", server.takeRequest().path)
    }

    @Test
    fun respectsRetryAfterCooldownUsingFakeClock() = runBlocking {
        var now = 1_000L
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "60"))
        server.enqueue(MockResponse().setBody("""{"now":1793827327001,"ac":[]}"""))
        val source = RadarRemoteSource(
            endpoints = listOf(RadarEndpoint("fonte", server.url("/radar").toString())),
            client = OkHttpClient(),
            clockMillis = { now },
            wait = {}
        )

        assertThrows(RadarUnavailableException::class.java) { runBlocking { source.fetchSnapshot() } }
        assertThrows(RadarUnavailableException::class.java) { runBlocking { source.fetchSnapshot() } }
        assertEquals(1, server.requestCount)
        now += 60_000L
        source.fetchSnapshot()
        assertEquals(2, server.requestCount)
    }
}
