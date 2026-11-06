package com.skywatch.cg.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.skywatch.cg.data.db.FollowedFlight
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
    fun schedule(context: Context, plan: AlertSchedulePlan, title: String, text: String) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerAtMillis, pendingIntent)
        }
    }

    fun eventAtMillis(flight: FollowedFlight): Long {
        val isArrival = flight.operationType == "ARRIVAL"
        val date = LocalDate.parse(if (isArrival) flight.arrivalDate else flight.flightDate)
        val utcMinutes = if (isArrival) flight.scheduledArrivalUtc else flight.scheduledDepartureUtc
        val time = LocalTime.ofSecondOfDay(utcMinutes * 60)
        val zone = ZoneId.of(if (isArrival) flight.destinationTimezone else flight.originTimezone)
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}
