package com.skywatch.cg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skywatch.cg.worker.SirosSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Receptor para BOOT_COMPLETED - garante que a sincronização periódica
 * seja re-agendada após reinicialização do dispositivo.
 * O WorkManager já persiste work requests automaticamente, mas este
 * receiver garante que as constraints sejam reaplicadas corretamente.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "BOOT_COMPLETED recebido - re-agendando sincronização periódica")
            schedulePeriodicSync(context)
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
}