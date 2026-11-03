package com.skywatch.cg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skywatch.cg.worker.FlightAlertWorker
import com.skywatch.cg.worker.SirosSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Receptor para BOOT_COMPLETED - garante que os workers periódicos
 * sejam re-agendados após reinicialização do dispositivo.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "BOOT_COMPLETED recebido - re-agendando workers periódicos")
            schedulePeriodicSync(context)
            scheduleAlertWorker(context)
        }
    }

    private fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SirosSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "siros_periodic_sync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
        Log.d("BootReceiver", "WorkManager: sincronização periódica re-agendada após boot (6 horas)")
    }

    private fun scheduleAlertWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val alertWorkRequest = PeriodicWorkRequestBuilder<FlightAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "flight_alert_check",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            alertWorkRequest
        )
        Log.d("BootReceiver", "WorkManager: verificação de alertas re-agendada após boot (15 min)")
    }
}