package com.skywatch.cg.data.update

import com.skywatch.cg.BuildConfig
import com.skywatch.cg.util.AppVersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    val version: String,
    val notes: String,
    val releaseUrl: String,
    val apkUrl: String?
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val release: GitHubRelease) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

class GitHubReleaseUpdateChecker(
    private val client: OkHttpClient = DEFAULT_CLIENT,
    private val installedVersion: String = BuildConfig.VERSION_NAME,
    private val apiUrl: String = LATEST_RELEASE_URL
) {
    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SkyWatchCG/1.0 (Android)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val message = if (response.code == 404) {
                        "O repositório de atualizações não está acessível. Verifique a publicação das releases."
                    } else {
                        "Não foi possível consultar as atualizações agora."
                    }
                    return@withContext UpdateCheckResult.Failed(
                        message
                    )
                }
                val body = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Failed(
                        "A resposta de atualização veio vazia."
                    )
                evaluateRelease(body)
            }
        } catch (_: IOException) {
            UpdateCheckResult.Failed("Sem conexão para verificar atualizações.")
        } catch (_: Exception) {
            UpdateCheckResult.Failed("Não foi possível ler a atualização disponível.")
        }
    }

    private fun evaluateRelease(json: String): UpdateCheckResult {
        val tagName = extractJsonString(json, "tag_name")?.trim().orEmpty()
        val releaseVersion = tagName.removePrefix("v").removePrefix("V")
        val comparison = AppVersionComparator.compare(releaseVersion, installedVersion)
            ?: return UpdateCheckResult.Failed("A versão publicada não é válida.")
        if (comparison <= 0) return UpdateCheckResult.UpToDate

        val assetsJson = json.substringAfter("\"assets\"", "")
        val apkUrl = Regex("\\{[^{}]*}", setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(assetsJson)
            .map { it.value }
            .firstOrNull { extractJsonString(it, "name") == RELEASE_APK_NAME }
            ?.let { extractJsonString(it, "browser_download_url") }

        return UpdateCheckResult.Available(
            GitHubRelease(
                version = releaseVersion,
                notes = extractJsonString(json, "body").orEmpty().trim(),
                releaseUrl = extractJsonString(json, "html_url").orEmpty().trim(),
                apkUrl = apkUrl
            )
        )
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex(
            "\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        )
        return pattern.find(json)?.groupValues?.get(1)?.let(::unescapeJson)
    }

    private fun unescapeJson(value: String): String = value
        .replace("\\\\", "\\")
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")

    companion object {
        const val OWNER = "mundoarkade1-arch"
        const val REPOSITORY = "sky-watch-cg"
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/$OWNER/$REPOSITORY/releases/latest"
        const val RELEASE_APK_NAME = "app-release.apk"

        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
