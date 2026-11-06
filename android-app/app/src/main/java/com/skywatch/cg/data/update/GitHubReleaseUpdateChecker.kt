package com.skywatch.cg.data.update

import com.skywatch.cg.BuildConfig
import com.skywatch.cg.util.AppVersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
                    return@withContext UpdateCheckResult.Failed(
                        "Não foi possível consultar as atualizações agora."
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
        val root = JSONObject(json)
        val tagName = root.optString("tag_name").trim()
        val releaseVersion = tagName.removePrefix("v").removePrefix("V")
        val comparison = AppVersionComparator.compare(releaseVersion, installedVersion)
            ?: return UpdateCheckResult.Failed("A versão publicada não é válida.")
        if (comparison <= 0) return UpdateCheckResult.UpToDate

        val assets = root.optJSONArray("assets")
        var apkUrl: String? = null
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("name") == RELEASE_APK_NAME) {
                    apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    break
                }
            }
        }
        return UpdateCheckResult.Available(
            GitHubRelease(
                version = releaseVersion,
                notes = root.optString("body").trim(),
                releaseUrl = root.optString("html_url").trim(),
                apkUrl = apkUrl
            )
        )
    }

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
