package com.skywatch.cg.data.siros

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Fonte remota para download do CSV de registros vigentes do SIROS/ANAC.
 * Endpoint: https://siros.anac.gov.br/siros/registros/registros/registros.csv
 */
class SirosRemoteSource(
    private val url: String = DEFAULT_URL,
    private val client: OkHttpClient = DEFAULT_CLIENT
) {
    companion object {
        const val DEFAULT_URL = "https://siros.anac.gov.br/siros/registros/registros/registros.csv"
        private const val DOWNLOAD_TIMEOUT_SECONDS = 120L
        private const val CONNECT_TIMEOUT_SECONDS = 30L

        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val INSTANCE = SirosRemoteSource()

        suspend fun downloadCsvToFile(tempFile: File): DownloadResult {
            return INSTANCE.downloadCsvToFileInstance(tempFile)
        }
    }

    suspend fun downloadCsvToFileInstance(tempFile: File): DownloadResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var response: Response? = null
            var bytesDownloaded = 0L

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SkyWatchCG/1.0 (Android)")
                    .build()

                response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    return@withContext DownloadResult.Failure(errorMsg, response.code)
                }

                val responseBody = response.body
                if (responseBody == null) {
                    return@withContext DownloadResult.Failure("Resposta sem corpo", response.code)
                }

                val contentLength = responseBody.contentLength()

                tempFile.parentFile?.mkdirs()
                val inputStream = responseBody.byteStream()
                val outputStream = FileOutputStream(tempFile)

                try {
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesDownloaded += read.toLong()
                    }
                    outputStream.flush()
                } finally {
                    outputStream.close()
                    inputStream.close()
                }

                if (bytesDownloaded == 0L) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext DownloadResult.Failure("Arquivo vazio", response.code)
                }

                if (contentLength > 0 && contentLength != bytesDownloaded) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext DownloadResult.Failure("Download incompleto", response.code)
                }

                val elapsedMs = System.currentTimeMillis() - startTime
                DownloadResult.Success(bytesDownloaded, elapsedMs, response.code)

            } catch (e: UnknownHostException) {
                val elapsedMs = System.currentTimeMillis() - startTime
                if (tempFile.exists()) tempFile.delete()
                DownloadResult.Failure("Sem conexão com a internet", 0, elapsedMs)
            } catch (e: SocketTimeoutException) {
                val elapsedMs = System.currentTimeMillis() - startTime
                if (tempFile.exists()) tempFile.delete()
                DownloadResult.Failure("Timeout ao baixar o CSV", 0, elapsedMs)
            } catch (e: IOException) {
                val elapsedMs = System.currentTimeMillis() - startTime
                if (tempFile.exists()) tempFile.delete()
                DownloadResult.Failure("Erro de I/O: ${e.message}", 0, elapsedMs)
            } catch (e: Exception) {
                val elapsedMs = System.currentTimeMillis() - startTime
                if (tempFile.exists()) tempFile.delete()
                DownloadResult.Failure("Erro inesperado: ${e.message}", 0, elapsedMs)
            } finally {
                response?.close()
            }
        }
    }

    sealed interface DownloadResult {
        data class Success(val bytesDownloaded: Long, val elapsedMs: Long, val httpCode: Int) : DownloadResult
        data class Failure(val errorMessage: String, val httpCode: Int = 0, val elapsedMs: Long = 0) : DownloadResult
    }
}