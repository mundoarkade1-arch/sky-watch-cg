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
object SirosRemoteSource {

    private const val SIROS_CSV_URL = "https://siros.anac.gov.br/siros/registros/registros/registros.csv"
    private const val DOWNLOAD_TIMEOUT_SECONDS = 120L
    private const val CONNECT_TIMEOUT_SECONDS = 30L

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun downloadCsvToFile(tempFile: File): DownloadResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var response: Response? = null
            var bytesDownloaded = 0L

            try {
                Log.d("SirosRemoteSource", "Iniciando download do CSV SIROS: $SIROS_CSV_URL")

                val request = Request.Builder()
                    .url(SIROS_CSV_URL)
                    .header("User-Agent", "SkyWatchCG/1.0 (Android)")
                    .build()

                response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    Log.e("SirosRemoteSource", "Falha no download: $errorMsg")
                    return@withContext DownloadResult.Failure(errorMsg, response.code)
                }

                val responseBody = response.body
                if (responseBody == null) {
                    Log.e("SirosRemoteSource", "Resposta HTTP sem corpo")
                    return@withContext DownloadResult.Failure("Resposta sem corpo", response.code)
                }

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

                val elapsedMs = System.currentTimeMillis() - startTime
                Log.d("SirosRemoteSource", "Download concluído: ${bytesDownloaded} bytes em ${elapsedMs}ms")
                DownloadResult.Success(bytesDownloaded, elapsedMs, response.code)

            } catch (e: UnknownHostException) {
                val elapsedMs = System.currentTimeMillis() - startTime
                Log.e("SirosRemoteSource", "Erro de DNS/sem internet: ${e.message}")
                DownloadResult.Failure("Sem conexão com a internet", 0, elapsedMs)
            } catch (e: SocketTimeoutException) {
                val elapsedMs = System.currentTimeMillis() - startTime
                Log.e("SirosRemoteSource", "Timeout no download: ${e.message}")
                DownloadResult.Failure("Timeout ao baixar o CSV", 0, elapsedMs)
            } catch (e: IOException) {
                val elapsedMs = System.currentTimeMillis() - startTime
                Log.e("SirosRemoteSource", "Erro de I/O no download: ${e.message}")
                DownloadResult.Failure("Erro de I/O: ${e.message}", 0, elapsedMs)
            } catch (e: Exception) {
                val elapsedMs = System.currentTimeMillis() - startTime
                Log.e("SirosRemoteSource", "Erro inesperado no download: ${e.message}")
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