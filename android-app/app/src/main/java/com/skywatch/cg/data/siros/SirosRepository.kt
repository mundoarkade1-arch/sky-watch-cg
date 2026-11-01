package com.skywatch.cg.data.siros

import android.util.Log
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.db.FlightEntity
import com.skywatch.cg.data.db.SyncMetadata
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SirosRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val dao = database.flightDao()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun saveNormalizedFlights(flights: List<NormalizedFlight>) = withContext(ioDispatcher) {
        Log.d("SirosRepositoryDiag", "saveNormalizedFlights called with ${flights.size} flights")
        val entities = flights.map { FlightEntity.fromNormalizedFlight(it) }
        dao.upsertAll(entities)
    }

    suspend fun getArrivalsByDate(date: LocalDate): List<FlightEntity> = withContext(ioDispatcher) {
        dao.getArrivalsByDate(date.format(dateFormatter))
    }

    suspend fun getDeparturesByDate(date: LocalDate): List<FlightEntity> = withContext(ioDispatcher) {
        dao.getDeparturesByDate(date.format(dateFormatter))
    }

    suspend fun getAllArrivals(): List<FlightEntity> = withContext(ioDispatcher) {
        dao.getAllArrivals()
    }

    suspend fun getAllDepartures(): List<FlightEntity> = withContext(ioDispatcher) {
        dao.getAllDepartures()
    }

    suspend fun getFlightsByDate(date: LocalDate): List<FlightEntity> = withContext(ioDispatcher) {
        dao.getFlightsByDate(date.format(dateFormatter))
    }

    suspend fun getFlightById(flightId: String): FlightEntity? = withContext(ioDispatcher) {
        dao.getByFlightId(flightId)
    }

    suspend fun getFlightsBySirosRecordId(sirosRecordId: String): List<FlightEntity> = withContext(ioDispatcher) {
        dao.getBySirosRecordId(sirosRecordId)
    }

    suspend fun deleteOldFlights(beforeDate: LocalDate): Int = withContext(ioDispatcher) {
        dao.deleteOldFlights(beforeDate.format(dateFormatter))
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        dao.deleteAll()
    }

    /**
     * Sincronização remota transacional:
     * 1. Baixa o CSV para arquivo temporário (via SirosRemoteSource).
     * 2. Faz o parsing streaming e normalização para SBCG (Aeroporto de Campo Grande).
     * 3. Substitui transacionalmente a grade de voos no banco (Room transaction) e atualiza sync_metadata.
     * Se falhar a qualquer momento, o banco antigo permanece intacto.
     */
    suspend fun syncFromRemote(): SyncResult = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        val dbPath = database.openHelper.writableDatabase.path ?: "."
        val parentDir = File(dbPath).parentFile ?: File(".")
        val tempFile = File(parentDir, "siros_temp_${System.currentTimeMillis()}.csv")

        try {
            Log.d("SirosRepository", "Iniciando syncFromRemote...")
            val downloadResult = SirosRemoteSource.downloadCsvToFile(tempFile)
            
            when (downloadResult) {
                is SirosRemoteSource.DownloadResult.Failure -> {
                    Log.e("SirosRepository", "Falha no download para sync: ${downloadResult.errorMessage}")
                    return@withContext SyncResult.Failure(downloadResult.errorMessage)
                }
                is SirosRemoteSource.DownloadResult.Success -> {
                    val records = mutableListOf<SirosRecord>()
                    var parseError: String? = null

                    SirosStreamParser.parseStreamWithCallback(
                        FileInputStream(tempFile),
                        onRecord = { records.add(it) },
                        onError = { line, err -> parseError = "Linha $line: $err" }
                    )

                    val currentError = parseError
                    if (currentError != null) {
                        Log.e("SirosRepository", "Falha no parse para sync: $currentError")
                        return@withContext SyncResult.Failure(currentError)
                    }

                    // Normaliza filtrando apenas para SBCG
                    val normalizedFlights = SirosNormalizer.normalize(records)
                    Log.d("SirosRepository", "Parsed e normalizados ${normalizedFlights.size} voos para SBCG")

                    // Executa transação Room com corrotina: limpa voos e insere novos, depois atualiza metadata
                    database.withTransaction {
                        dao.deleteAll()
                        val entities = normalizedFlights.map { FlightEntity.fromNormalizedFlight(it) }
                        dao.upsertAll(entities)

                        val metadataDao = database.syncMetadataDao()
                        val now = System.currentTimeMillis()
                        metadataDao.upsertSyncMetadata(
                            SyncMetadata(
                                id = 1,
                                lastSuccessfulSyncAt = now,
                                lastSyncStatus = "SUCCESS",
                                lastSyncErrorMessage = null,
                                recordsInserted = entities.size
                            )
                        )
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d("SirosRepository", "syncFromRemote concluído com sucesso em ${elapsed}ms")
                    SyncResult.Success(normalizedFlights.size, elapsed)
                }
            }
        } catch (e: Exception) {
            Log.e("SirosRepository", "Exceção em syncFromRemote: ${e.message}", e)
            SyncResult.Failure(e.localizedMessage ?: "Erro desconhecido")
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    suspend fun getSyncMetadata(): com.skywatch.cg.data.db.SyncMetadata? = withContext(ioDispatcher) {
        database.syncMetadataDao().getSyncMetadata()
    }

    sealed interface SyncResult {
        data class Success(val totalSynced: Int, val elapsedMs: Long) : SyncResult
        data class Failure(val errorMessage: String) : SyncResult
    }

    suspend fun getFlightsForUi(date: LocalDate): List<NormalizedFlight> = withContext(ioDispatcher) {
        val entities = dao.getFlightsByDate(date.format(dateFormatter))
        entities.map { it.toNormalizedFlight() }
    }

    suspend fun getArrivalsForUi(date: LocalDate): List<NormalizedFlight> = withContext(ioDispatcher) {
        val formattedDate = date.format(dateFormatter)
        Log.d("SirosRepositoryDiag", "getArrivalsForUi queried for date: $formattedDate")
        val entities = dao.getArrivalsByDate(formattedDate)
        Log.d("SirosRepositoryDiag", "getArrivalsForUi returned ${entities.size} entities for date: $formattedDate")
        entities.map { it.toNormalizedFlight() }
    }

    suspend fun getDeparturesForUi(date: LocalDate): List<NormalizedFlight> = withContext(ioDispatcher) {
        val formattedDate = date.format(dateFormatter)
        Log.d("SirosRepositoryDiag", "getDeparturesForUi queried for date: $formattedDate")
        val entities = dao.getDeparturesByDate(formattedDate)
        Log.d("SirosRepositoryDiag", "getDeparturesForUi returned ${entities.size} entities for date: $formattedDate")
        entities.map { it.toNormalizedFlight() }
    }
}