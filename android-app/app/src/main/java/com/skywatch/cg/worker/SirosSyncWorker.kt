package com.skywatch.cg.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.siros.SirosRepository

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
                    Log.d("SirosSyncWorker", "Sincronização periódica concluída com sucesso: ${result.totalSynced} voos.")
                    Result.success()
                }
                is SirosRepository.SyncResult.Failure -> {
                    Log.e("SirosSyncWorker", "Falha na sincronização periódica: ${result.errorMessage}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("SirosSyncWorker", "Exceção na sincronização periódica: ${e.message}", e)
            Result.retry()
        }
    }
}