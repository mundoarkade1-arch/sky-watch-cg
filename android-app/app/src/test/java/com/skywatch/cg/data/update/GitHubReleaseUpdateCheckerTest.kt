package com.skywatch.cg.data.update

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GitHubReleaseUpdateCheckerTest {
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
    fun reportsNewReleaseAndStableApkUrl() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(releaseJson("1.0.10")))

        val result = checker(installedVersion = "1.0.9").check()

        assertTrue(result is UpdateCheckResult.Available)
        val release = (result as UpdateCheckResult.Available).release
        assertEquals("1.0.10", release.version)
        assertEquals("https://example.test/app-release.apk", release.apkUrl)
    }

    @Test
    fun reportsUpToDateWhenPublishedVersionIsNotNewer() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(releaseJson("1.0.9")))

        assertEquals(UpdateCheckResult.UpToDate, checker(installedVersion = "1.0.10").check())
    }

    @Test
    fun reportsFriendlyFailureForOfflineResponse() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        assertTrue(checker(installedVersion = "1.0.9").check() is UpdateCheckResult.Failed)
    }

    private fun checker(installedVersion: String): GitHubReleaseUpdateChecker {
        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
        return GitHubReleaseUpdateChecker(
            client = client,
            installedVersion = installedVersion,
            apiUrl = server.url("/releases/latest").toString()
        )
    }

    private fun releaseJson(version: String): String = """
        {
          "tag_name": "v$version",
          "body": "Notas da versão",
          "html_url": "https://example.test/releases/$version",
          "assets": [
            {"name": "app-release.apk", "browser_download_url": "https://example.test/app-release.apk"}
          ]
        }
    """.trimIndent()
}
