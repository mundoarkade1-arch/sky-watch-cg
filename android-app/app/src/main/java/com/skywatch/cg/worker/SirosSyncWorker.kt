package com.skywatch.cg.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.siros.SirosRepository
import com.skywatch.cg.notification.FlightAlertScheduler
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Worker do WorkManager para atualização periódica em segundo plano dos dados do SIROS.
 * Executa periodicamente a sincronização remota via SirosRepository.
 */
class SirosSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("SirosSyncWorker", "Iniciando sincronização periódica via WorkManager...")
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SirosRepository(database)

        return try {
            val result = repository.syncFromRemote()
            when (result) {
                is SirosRepository.SyncResult.Success -> {
                    database.alertSentDao().deleteOldAlerts(
                        Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
                    )
                    val flights = database.flightDao().getAllArrivals().map { it.toNormalizedFlight() } +
                        database.flightDao().getAllDepartures().map { it.toNormalizedFlight() }
                    FlightAlertScheduler.scheduleUpcomingFlights(applicationContext, flights)
                    Log.d("SirosSyncWorker", "Sincronização periódica concluída com sucesso: ${result.totalSynced} voos.")
                    Result.success()
                }
                is SirosRepository.SyncResult.Failure -> {
                    Log.e("SirosSyncWorker", "Falha na sincronização periódica: ${result.errorMessage}")
                    // Após 5 tentativas, falha definitivamente; antes, retry
                    if (runAttemptCount >= 5) {
                        Log.e("SirosSyncWorker", "Máximo de tentativas (5) atingido. Falha definitiva.")
                        Result.failure()
                    } else {
                        Log.d("SirosSyncWorker", "Tentativa ${runAttemptCount}/5 falhou. Agendando retry.")
                        Result.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SirosSyncWorker", "Exceção na sincronização periódica: ${e.message}", e)
            if (runAttemptCount >= 5) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}
