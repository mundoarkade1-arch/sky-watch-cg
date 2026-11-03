package com.skywatch.cg.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.skywatch.cg.MainActivity
import com.skywatch.cg.R
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.util.getDisplayTime

/**
 * Helper para criar e gerenciar notificações de alerta de voo.
 */
object NotificationHelper {

    const val CHANNEL_ID = "flight_alerts"
    const val CHANNEL_NAME = "Alertas de Voo"
    const val NOTIFICATION_ID_BASE = 1000

    /**
     * Cria o canal de notificação (deve ser chamado uma vez na inicialização do app).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas para voos com partida/chegada próxima"
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Verifica se a permissão de notificação foi concedida (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true // Android < 13 não precisa de permissão runtime
    }

    /**
     * Solicita permissão de notificação (Android 13+) — deprecated, usar ActivityResult API.
     */
    @Deprecated("Use ActivityResultContracts.RequestPermission via MainActivity", ReplaceWith(""))
    fun requestNotificationPermission(activity: android.app.Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
        }
    }

    /**
     * Mostra notificação de alerta de voo.
     */
    fun showFlightAlert(
        context: Context,
        flight: NormalizedFlight,
        minutesUntilEvent: Int,
        isArrival: Boolean
    ) {
        val eventType = if (isArrival) "chegada" else "partida"
        val title = "${flight.airline} ${flight.flightNumber} - ${eventType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }} em $minutesUntilEvent min"
        val location = if (isArrival) {
            "Origem: ${flight.origin} (${flight.originName ?: ""}) → ${flight.destination}"
        } else {
            "Origem: ${flight.origin} → Destino: ${flight.destination} (${flight.destinationName ?: ""})"
        }
        val timeText = if (isArrival) {
            "Horário local: ${flight.getDisplayTime(true)}"
        } else {
            "Horário local: ${flight.getDisplayTime(false)}"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_flight)
            .setContentTitle(title)
            .setContentText("$location\n$timeText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$location\n$timeText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // ID baseado no hash do flightId (único por voo+data), não só no número do voo
        val notificationId = flight.flightId.hashCode()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    /**
     * Cancela notificação de um voo específico pelo flightId.
     */
    fun cancelFlightAlert(context: Context, flightId: String) {
        val notificationId = flightId.hashCode()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}