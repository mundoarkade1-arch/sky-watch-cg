package com.skywatch.cg.data.radar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class RadarRemoteSource(
    private val url: String = DEFAULT_URL,
    private val client: OkHttpClient = DEFAULT_CLIENT
) {
    suspend fun fetchSnapshot(): RadarSnapshot = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SKYWATCH-CG/1.0 (experimental ADS-B radar)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Fonte ADS-B respondeu HTTP ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Fonte ADS-B retornou resposta vazia")
            RadarSnapshotParser.parse(body, CENTER_LATITUDE, CENTER_LONGITUDE)
        }
    }

    companion object {
        const val CENTER_LATITUDE = -20.4686
        const val CENTER_LONGITUDE = -54.6724
        const val RANGE_NAUTICAL_MILES = 50
        const val DEFAULT_URL =
            "https://api.adsb.lol/v2/lat/$CENTER_LATITUDE/lon/$CENTER_LONGITUDE/dist/$RANGE_NAUTICAL_MILES"

        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
