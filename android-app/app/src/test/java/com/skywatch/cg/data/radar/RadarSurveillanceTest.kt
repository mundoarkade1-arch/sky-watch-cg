package com.skywatch.cg.data.radar

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RadarSurveillanceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() { server = MockWebServer().also { it.start() } }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun pollsEveryTenSecondsWithFakeClock() = runBlocking {
        repeat(3) { server.enqueue(MockResponse().setBody("""{"now":1793827327001,"ac":[]}""")) }
        var now = 1_000L
        var polls = 0
        val source = RadarRemoteSource(server.url("/radar").toString(), OkHttpClient())
        val surveillance = RadarSurveillance(
            RadarRepository(source),
            clockMillis = { now },
            wait = { interval ->
                assertEquals(10_000L, interval)
                now += interval
                if (++polls == 3) throw CancellationException("fim do teste")
            }
        )

        try {
            surveillance.run(isStarted = { true }, isEnabled = { true }, onSnapshot = { _, observedAt ->
                assertTrue(observedAt >= 1_000L)
            })
        } catch (_: CancellationException) { }

        assertEquals(3, server.requestCount)
    }

    @Test
    fun doesNotPollWhileOutsideStartedOrWhenDisabled() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"now":1793827327001,"ac":[]}"""))
        var started = false
        var enabled = true
        var waits = 0
        val surveillance = RadarSurveillance(
            RadarRepository(RadarRemoteSource(server.url("/radar").toString(), OkHttpClient())),
            wait = {
                waits++
                when (waits) {
                    1 -> started = true
                    2 -> enabled = false
                    else -> throw CancellationException("fim do teste")
                }
            }
        )

        try {
            surveillance.run(isStarted = { started }, isEnabled = { enabled }, onSnapshot = { _, _ -> })
        } catch (_: CancellationException) { }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun pausesPollingWhenLifecycleMovesFromStartedBackToCreated() = runBlocking {
        val owner = TestLifecycleOwner()
        var calls = 0
        val surveillance = RadarSurveillance(
            repository = RadarRepository(),
            intervalMillis = 10_000L,
            wait = { delay(1) },
            refreshSnapshot = {
                calls++
                RadarSnapshot(1_000L, emptyList(), "teste")
            }
        )
        val job = launch {
            surveillance.runForLifecycle(
                lifecycle = owner.lifecycle,
                isEnabled = { true },
                onSnapshot = { _, _ -> }
            )
        }

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        delay(10)
        assertEquals(0, calls)

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        delay(20)
        assertTrue(calls > 0)
        val callsWhenStarted = calls

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        delay(20)
        assertEquals(callsWhenStarted, calls)

        job.cancel()
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry
    }
}
