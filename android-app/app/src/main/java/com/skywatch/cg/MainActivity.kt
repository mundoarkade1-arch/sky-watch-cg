package com.skywatch.cg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skywatch.cg.notification.NotificationHelper
import com.skywatch.cg.notification.FlightAlertScheduler
import com.skywatch.cg.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.skywatch.cg.ui.screens.MainScreen
import com.skywatch.cg.ui.theme.SkyWatchTheme
import com.skywatch.cg.ui.viewmodel.FlightListViewModel
import com.skywatch.cg.ui.viewmodel.RadarViewModel
import com.skywatch.cg.worker.SirosSyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: FlightListViewModel by viewModels()
    private val radarViewModel: RadarViewModel by viewModels()
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.startAutoRefresh(lifecycle)
        radarViewModel.startSurveillance(lifecycle)
        NotificationHelper.createNotificationChannel(this)
        if (!NotificationHelper.hasNotificationPermission(this)) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        schedulePeriodicSync()
        scheduleUpcomingAlerts()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SkyWatchTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel, radarViewModel)
                }
            }
        }
    }

    private fun scheduleUpcomingAlerts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            val flights = database.flightDao().getAllArrivals().map { it.toNormalizedFlight() } +
                database.flightDao().getAllDepartures().map { it.toNormalizedFlight() }
            FlightAlertScheduler.scheduleUpcomingFlights(applicationContext, flights)
        }
    }

    private fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SirosSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "siros_periodic_sync", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}
