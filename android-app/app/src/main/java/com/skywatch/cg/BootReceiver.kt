package com.skywatch.cg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skywatch.cg.worker.SirosSyncWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val request = PeriodicWorkRequestBuilder<SirosSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "siros_periodic_sync", ExistingPeriodicWorkPolicy.KEEP, request
        )
        Log.d("BootReceiver", "BOOT_COMPLETED recebido; sincronização SIROS reagendada")
        // AlarmManager mantém cada alerta individual; o receiver é persistente após boot.
    }
}
