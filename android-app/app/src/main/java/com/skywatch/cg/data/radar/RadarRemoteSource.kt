package com.skywatch.cg.data.radar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class RadarEndpoint(val name: String, val url: String)

class RadarUnavailableException(val retryAfterSeconds: Long?, details: String) :
    IOException("Fontes indisponíveis: $details")

private class RadarHttpException(
    val statusCode: Int,
    val retryAfterMillis: Long?,
    message: String
) : IOException(message)

private class EndpointState {
    var lastRequestMillis = 0L
    var cooldownUntilMillis = 0L
    var nextBackoffMillis = 60_000L
    fun resetAfterSuccess() {
        cooldownUntilMillis = 0L
        nextBackoffMillis = 60_000L
    }
}

class RadarRemoteSource(
    private val endpoints: List<RadarEndpoint> = DEFAULT_ENDPOINTS,
    private val client: OkHttpClient = DEFAULT_CLIENT,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val wait: suspend (Long) -> Unit = { delay(it) }
) {
    constructor(url: String, client: OkHttpClient) : this(
        endpoints = listOf(RadarEndpoint("teste", url)), client = client
    )

    private val state = ConcurrentHashMap<String, EndpointState>()

    suspend fun fetchSnapshot(): RadarSnapshot = withContext(Dispatchers.IO) {
        val failures = mutableListOf<String>()
        for (endpoint in endpoints) {
            val endpointState = state.getOrPut(endpoint.name) { EndpointState() }
            val now = clockMillis()
            if (endpointState.cooldownUntilMillis > now) {
                failures += "${endpoint.name} em cooldown"
                continue
            }
            val sinceLastRequest = now - endpointState.lastRequestMillis
            if (endpointState.lastRequestMillis != 0L && sinceLastRequest < MIN_REQUEST_INTERVAL_MILLIS) {
                wait(MIN_REQUEST_INTERVAL_MILLIS - sinceLastRequest)
            }
            endpointState.lastRequestMillis = clockMillis()
            try {
                val snapshot = request(endpoint)
                endpointState.resetAfterSuccess()
                return@withContext snapshot.copy(sourceName = endpoint.name)
            } catch (error: RadarHttpException) {
                if (error.statusCode == HTTP_TOO_MANY_REQUESTS) {
                    val cooldown = error.retryAfterMillis ?: endpointState.nextBackoffMillis
                    endpointState.cooldownUntilMillis = clockMillis() + cooldown
                    endpointState.nextBackoffMillis = (cooldown * 2).coerceAtMost(MAX_COOLDOWN_MILLIS)
                }
                failures += "${endpoint.name}: ${error.message}"
            } catch (error: IOException) {
                failures += "${endpoint.name}: ${error.message ?: "falha de rede"}"
            }
        }
        val now = clockMillis()
        val retryAt = state.values.map { it.cooldownUntilMillis }.filter { it > now }.minOrNull()
        val seconds = retryAt?.let { ((it - now) + 999L) / 1000L }
        throw RadarUnavailableException(seconds, failures.joinToString("; "))
    }

    private fun request(endpoint: RadarEndpoint): RadarSnapshot {
        val request = Request.Builder().url(endpoint.url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val retryAfter = response.header("Retry-After")?.toLongOrNull()
                    ?.takeIf { it >= 0L }?.times(1000L)
                throw RadarHttpException(response.code, retryAfter, "Fonte ADS-B respondeu HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Fonte ADS-B retornou resposta vazia")
            return RadarSnapshotParser.parse(body, CENTER_LATITUDE, CENTER_LONGITUDE)
        }
    }

    companion object {
        const val CENTER_LATITUDE = -20.4686
        const val CENTER_LONGITUDE = -54.6724
        const val RANGE_NAUTICAL_MILES = 50
        const val USER_AGENT = "SkyWatchCG/1.0 (app pessoal)"
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val MIN_REQUEST_INTERVAL_MILLIS = 1_000L
        const val MAX_COOLDOWN_MILLIS = 5 * 60 * 1_000L
        const val DEFAULT_URL = "https://api.adsb.lol/v2/point/$CENTER_LATITUDE/$CENTER_LONGITUDE/$RANGE_NAUTICAL_MILES"
        val DEFAULT_ENDPOINTS = listOf(
            RadarEndpoint("adsb.lol", DEFAULT_URL),
            RadarEndpoint("airplanes.live", "https://api.airplanes.live/v2/point/$CENTER_LATITUDE/$CENTER_LONGITUDE/$RANGE_NAUTICAL_MILES"),
            RadarEndpoint("adsb.fi", "https://opendata.adsb.fi/api/v2/lat/$CENTER_LATITUDE/lon/$CENTER_LONGITUDE/dist/$RANGE_NAUTICAL_MILES")
        )
        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).build()
    }
}
