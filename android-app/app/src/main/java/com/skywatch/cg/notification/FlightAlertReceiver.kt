package com.skywatch.cg.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.skywatch.cg.data.db.AlertSent
import com.skywatch.cg.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlightAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: return@launch
                if (!NotificationHelper.hasNotificationPermission(context)) return@launch
                val database = AppDatabase.getDatabase(context.applicationContext)
                NotificationHelper.createNotificationChannel(context)
                val displayed = NotificationHelper.showTextAlert(
                    context,
                    intent.getStringExtra(EXTRA_TITLE) ?: "Alerta de voo",
                    intent.getStringExtra(EXTRA_TEXT).orEmpty(),
                    alertId.hashCode()
                )
                if (displayed) {
                    database.alertSentDao().insertIfAbsent(
                        AlertSent(alertId, intent.getStringExtra(EXTRA_FLIGHT_ID).orEmpty(), "SCHEDULED", "", 0, System.currentTimeMillis())
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_FLIGHT_ID = "flight_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
