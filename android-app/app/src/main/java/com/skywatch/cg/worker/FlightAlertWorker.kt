package com.skywatch.cg.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.db.AlertSent
import com.skywatch.cg.data.prefs.AppPreferences
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.notification.NotificationHelper
import com.skywatch.cg.util.getAirportEventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Worker para enviar uma notificação por evento quando o voo estiver entre 15 e 30 minutos.
 */
class FlightAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val campoGrandeZone = ZoneId.of("America/Campo_Grande")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("FlightAlertWorker", "Iniciando verificação de voos próximos...")

        try {
            val database = AppDatabase.getDatabase(applicationContext)
            if (!shouldProcessAlerts(AppPreferences.getInstance(applicationContext).alertsEnabled)) {
                return@withContext Result.success()
            }
            val now = LocalDateTime.now(campoGrandeZone)
            val today = now.toLocalDate()
            database.alertSentDao().deleteOldAlerts(
                Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
            )

            if (!NotificationHelper.hasNotificationPermission(applicationContext)) {
                Log.w("FlightAlertWorker", "Permissão de notificações não concedida; alertas ignorados")
                return@withContext Result.success()
            }

            val arrivalsToday = getFlightsForDate(database, today, OperationType.ARRIVAL)
            val departuresToday = getFlightsForDate(database, today, OperationType.DEPARTURE)

            val tomorrow = today.plusDays(1)
            val arrivalsTomorrow = getFlightsForDate(database, tomorrow, OperationType.ARRIVAL)
            val departuresTomorrow = getFlightsForDate(database, tomorrow, OperationType.DEPARTURE)

            val allFlights = arrivalsToday + departuresToday + arrivalsTomorrow + departuresTomorrow

            var alertsSent = 0

            for (flight in allFlights) {
                val eventLocalDateTime = flight.getAirportEventDateTime()

                val minutesUntil = ChronoUnit.MINUTES.between(now, eventLocalDateTime).toInt()

                if (isWithinAlertWindow(minutesUntil.toLong())) {
                    val isArrival = flight.operationType == OperationType.ARRIVAL
                    val alertId = alertId(
                        flight.flightId,
                        flight.operationType,
                        eventLocalDateTime.toLocalDate()
                    )
                    if (database.alertSentDao().isAlertSent(alertId)) continue

                    val displayed = NotificationHelper.showFlightAlert(
                        applicationContext,
                        flight,
                        minutesUntil.toInt(),
                        isArrival
                    )
                    if (displayed) {
                        database.alertSentDao().insertIfAbsent(
                            AlertSent(
                                id = alertId,
                                flightId = flight.flightId,
                                operationType = flight.operationType.name,
                                eventDate = eventLocalDateTime.toLocalDate().toString(),
                                minutesBefore = minutesUntil,
                                sentAt = Instant.now().toEpochMilli()
                            )
                        )
                        alertsSent++
                    }
                    Log.d("FlightAlertWorker", "Alerta enviado: ${flight.airline} ${flight.flightNumber} em ${minutesUntil} min (${if (isArrival) "chegada" else "partida"})")
                }
            }

            Log.d("FlightAlertWorker", "Verificação concluída: $alertsSent alertas enviados")
            Result.success()

        } catch (e: Exception) {
            Log.e("FlightAlertWorker", "Erro ao verificar voos próximos: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        const val EARLIEST_ALERT_MINUTES = 15L
        const val LATEST_ALERT_MINUTES = 30L

        internal fun isWithinAlertWindow(minutesUntil: Long): Boolean =
            minutesUntil in EARLIEST_ALERT_MINUTES..LATEST_ALERT_MINUTES

        internal fun shouldProcessAlerts(alertsEnabled: Boolean): Boolean = alertsEnabled

        internal fun eventUtcDate(
            flightDate: LocalDate,
            arrivalDate: LocalDate,
            operationType: OperationType
        ): LocalDate = if (operationType == OperationType.ARRIVAL) arrivalDate else flightDate

        internal fun alertId(
            flightId: String,
            operationType: OperationType,
            eventDate: LocalDate
        ): String = "$flightId:${operationType.name}:$eventDate"
    }

    private suspend fun getFlightsForDate(database: AppDatabase, date: LocalDate, operationType: OperationType): List<NormalizedFlight> = withContext(Dispatchers.IO) {
        val dao = database.flightDao()
        val formattedDate = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val entities = if (operationType == OperationType.ARRIVAL) {
            dao.getArrivalsByDate(formattedDate)
        } else {
            dao.getDeparturesByDate(formattedDate)
        }
        entities.map { it.toNormalizedFlight() }
    }
}
