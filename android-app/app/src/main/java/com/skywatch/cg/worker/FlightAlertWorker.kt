package com.skywatch.cg.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Worker para verificar voos próximos e enviar notificações de alerta.
 * Executa periodicamente (ex: a cada 15-30 min) para checar voos com partida/chegada
 * nas próximas 2 horas e enviar alertas.
 */
class FlightAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val campoGrandeZone = ZoneId.of("America/Campo_Grande")
    private val saoPauloZone = ZoneId.of("America/Sao_Paulo")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("FlightAlertWorker", "Iniciando verificação de voos próximos...")

        val database = AppDatabase.getDatabase(applicationContext)
        val now = LocalDateTime.now(campoGrandeZone)
        val today = now.toLocalDate()
        val twoHoursFromNow = now.plusHours(2)

        try {
            // Busca voos de hoje e amanhã (para cobrir virada de dia)
            val arrivalsToday = getFlightsForDate(database, today, OperationType.ARRIVAL)
            val departuresToday = getFlightsForDate(database, today, OperationType.DEPARTURE)
            
            val tomorrow = today.plusDays(1)
            val arrivalsTomorrow = getFlightsForDate(database, tomorrow, OperationType.ARRIVAL)
            val departuresTomorrow = getFlightsForDate(database, tomorrow, OperationType.DEPARTURE)

            val allFlights = arrivalsToday + departuresToday + arrivalsTomorrow + departuresTomorrow

            var alertsSent = 0

            for (flight in allFlights) {
                val eventTimeUtc = if (flight.operationType == OperationType.ARRIVAL) {
                    flight.scheduledArrivalUtc
                } else {
                    flight.scheduledDepartureUtc
                }

                // Converte UTC para local (timezone do aeroporto)
                val eventLocalDateTime = eventTimeUtc.atDate(flight.flightDate)
                    .atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(if (flight.operationType == OperationType.ARRIVAL) {
                        ZoneId.of(flight.destinationTimezone)
                    } else {
                        ZoneId.of(flight.originTimezone)
                    })
                    .toLocalDateTime()

                // Verifica se o evento está nas próximas 2 horas
                val minutesUntil = ChronoUnit.MINUTES.between(now, eventLocalDateTime).toInt()
                
                if (minutesUntil > 0 && minutesUntil <= 120) {
                    // Envia notificação se ainda não foi enviada
                    val isArrival = flight.operationType == OperationType.ARRIVAL
                    NotificationHelper.showFlightAlert(
                        applicationContext,
                        flight,
                        minutesUntil.toInt(),
                        isArrival
                    )
                    alertsSent++
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