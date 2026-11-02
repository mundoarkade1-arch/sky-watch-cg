package com.skywatch.cg.data.siros

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SirosRemoteSourceTest {

    @Test
    fun testDownloadResultStructures() {
        val success = SirosRemoteSource.DownloadResult.Success(1000L, 250L, 200)
        assertEquals(1000L, success.bytesDownloaded)
        assertEquals(250L, success.elapsedMs)
        assertEquals(200, success.httpCode)

        val failure = SirosRemoteSource.DownloadResult.Failure("Erro de rede", 500, 100L)
        assertEquals("Erro de rede", failure.errorMessage)
        assertEquals(500, failure.httpCode)
        assertEquals(100L, failure.elapsedMs)
    }

    @Test
    fun testSyncFromRemoteSuccessAndFailures() {
        // Can be tested via SirosRepositoryTest or a dedicated integration test
    }

}