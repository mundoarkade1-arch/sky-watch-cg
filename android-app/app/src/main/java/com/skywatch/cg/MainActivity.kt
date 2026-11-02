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
}

