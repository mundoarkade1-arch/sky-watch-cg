package com.skywatch.cg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skywatch.cg.notification.NotificationHelper
import com.skywatch.cg.ui.screens.MainScreen
import com.skywatch.cg.ui.viewmodel.FlightListViewModel
import com.skywatch.cg.worker.FlightAlertWorker
import com.skywatch.cg.worker.SirosSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: FlightListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Cria o canal de notificação para alertas de voo
        NotificationHelper.createNotificationChannel(this)

        // Solicita permissão de notificação (Android 13+)
        if (!NotificationHelper.hasNotificationPermission(this)) {
            NotificationHelper.requestNotificationPermission(this)
        }
        
        // Agenda sincronização periódica via WorkManager (a cada 6 horas, requer rede)
        schedulePeriodicSync()
        
        // Verifica atualizações no GitHub Releases
        checkForAppUpdates()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SirosSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "siros_periodic_sync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
        android.util.Log.d("MainActivity", "WorkManager: sincronização periódica agendada (6 horas)")

        // Agenda verificação de alertas de voo (a cada 15 minutos, requer rede)
        val alertConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val alertWorkRequest = PeriodicWorkRequestBuilder<FlightAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(alertConstraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "flight_alert_check",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            alertWorkRequest
        )
        android.util.Log.d("MainActivity", "WorkManager: verificação de alertas agendada (15 min)")
    }

    private fun checkForAppUpdates() {
        val currentVersion = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = java.net.URL("https://api.github.com/repos/mundoarkade1-arch/sky-watch-cg/releases/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "SkyWatchCG-App")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // Parse simples de tag_name (ex: "tag_name":"v1.0.1")
                    val tagRegex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    val matchResult = tagRegex.find(response)
                    if (matchResult != null) {
                        val latestTag = matchResult.groupValues[1]
                        if (latestTag != "v$currentVersion" && latestTag != currentVersion) {
                            withContext(Dispatchers.Main) {
                                showUpdateDialog(latestTag)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Erro ao verificar atualização: ${e.message}")
            }
        }
    }

    private fun showUpdateDialog(newVersion: String) {
        try {
            android.app.AlertDialog.Builder(this)
                .setTitle("Nova versão disponível ($newVersion)")
                .setMessage("Uma nova versão do Sky Watch CG está disponível no GitHub. Deseja baixar e atualizar agora?")
                .setPositiveButton("Atualizar") { _, _ ->
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/mundoarkade1-arch/sky-watch-cg/releases/latest")
                    )
                    startActivity(intent)
                }
                .setNegativeButton("Depois", null)
                .show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao exibir diálogo de atualização: ${e.message}")
        }
    }
}

