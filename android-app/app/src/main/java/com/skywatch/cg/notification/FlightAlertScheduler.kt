package com.skywatch.cg.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.skywatch.cg.data.db.FollowedFlight
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.util.getDisplayDateTime
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.max

data class AlertSchedulePlan(
    val flightId: String,
    val eventAtMillis: Long,
    val minutesBefore: Int
) {
    val triggerAtMillis: Long get() = eventAtMillis - minutesBefore * 60_000L
    val alertId: String get() = "$flightId:$minutesBefore"
}

object FlightAlertScheduleLogic {
    fun plan(flightId: String, eventAtMillis: Long, minutesBefore: Int): AlertSchedulePlan =
        AlertSchedulePlan(flightId, eventAtMillis, minutesBefore.coerceAtLeast(0))
}

object FlightAlertScheduler {
    const val DEFAULT_MINUTES_BEFORE = 15
    private const val SCHEDULE_HORIZON_MILLIS = 48 * 60 * 60 * 1_000L

    fun schedule(
        context: Context,
        plan: AlertSchedulePlan,
        title: String,
        text: String,
        alarmAtMillis: Long = plan.triggerAtMillis
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FlightAlertReceiver::class.java).apply {
            putExtra(FlightAlertReceiver.EXTRA_ALERT_ID, plan.alertId)
            putExtra(FlightAlertReceiver.EXTRA_FLIGHT_ID, plan.flightId)
            putExtra(FlightAlertReceiver.EXTRA_TITLE, title)
            putExtra(FlightAlertReceiver.EXTRA_TEXT, text)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, plan.flightId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (shouldUseExactAlarm(Build.VERSION.SDK_INT, canScheduleExact)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmAtMillis, pendingIntent)
        }
    }

    internal fun shouldUseExactAlarm(apiLevel: Int, canScheduleExactAlarms: Boolean): Boolean =
        apiLevel < Build.VERSION_CODES.S || canScheduleExactAlarms

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }

    fun eventAtMillis(flight: FollowedFlight): Long {
        val isArrival = flight.operationType == "ARRIVAL"
        val date = if (isArrival) flight.arrivalDate else flight.flightDate
        val utcMinutes = if (isArrival) flight.scheduledArrivalUtc else flight.scheduledDepartureUtc
        return LocalDateTime.of(java.time.LocalDate.parse(date), java.time.LocalTime.ofSecondOfDay(utcMinutes * 60))
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }

    fun eventAtMillis(flight: NormalizedFlight): Long {
        val date = if (flight.operationType == OperationType.ARRIVAL) flight.arrivalDate else flight.flightDate
        val time = if (flight.operationType == OperationType.ARRIVAL) flight.scheduledArrivalUtc else flight.scheduledDepartureUtc
        return LocalDateTime.of(date, time).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    /** Agenda alertas para movimentos SIROS futuros já persistidos no cache local. */
    fun scheduleUpcomingFlights(
        context: Context,
        flights: Iterable<NormalizedFlight>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        var scheduled = 0
        flights.forEach { flight ->
            val eventAt = eventAtMillis(flight)
            val triggerAt = max(nowMillis + 1_000L, eventAt - DEFAULT_MINUTES_BEFORE * 60_000L)
            if (eventAt > nowMillis && eventAt <= nowMillis + SCHEDULE_HORIZON_MILLIS) {
                val eventType = if (flight.operationType == OperationType.ARRIVAL) "Pouso" else "Decolagem"
                val route = if (flight.operationType == OperationType.ARRIVAL) {
                    "Rota planejada: ${flight.origin} → CGR"
                } else {
                    "Rota planejada: CGR → ${flight.destination}"
                }
                schedule(
                    context = context,
                    plan = FlightAlertScheduleLogic.plan(flight.flightId, eventAt, DEFAULT_MINUTES_BEFORE),
                    title = "${flight.airline} ${flight.flightNumber} · $eventType programado",
                    text = "$route\nHorário programado local: ${flight.getDisplayDateTime()}",
                    alarmAtMillis = triggerAt
                )
                scheduled++
            }
        }
        return scheduled
    }
}
