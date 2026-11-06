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
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.notification.FlightAlertScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context.applicationContext)
                val flights = database.flightDao().getAllArrivals().map { it.toNormalizedFlight() } +
                    database.flightDao().getAllDepartures().map { it.toNormalizedFlight() }
                FlightAlertScheduler.scheduleUpcomingFlights(context.applicationContext, flights)
            } finally {
                pendingResult.finish()
            }
        }
        Log.d("BootReceiver", "BOOT_COMPLETED recebido; sincronização SIROS reagendada")
        Log.d("BootReceiver", "Alertas de voos futuros restaurados após boot")
    }
}
