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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val remoteSource: SirosRemoteSource = SirosRemoteSource.INSTANCE
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
     * 2. Faz o parsing streaming e normalização filtrando SBCG (sem acumular tudo).
     * 3. Se 0 voos de SBCG -> Failure sem tocar na grade.
     * 4. Substitui transacionalmente a grade de voos no banco (Room transaction) e atualiza sync_metadata.
     * Se falhar a qualquer momento, o banco antigo permanece intacto.
     * Em qualquer Failure grava em sync_metadata (id=1) lastSyncStatus="FAILURE" preservando lastSuccessfulSyncAt e recordsInserted anteriores.
     */
    suspend fun syncFromRemote(): SyncResult = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        val dbPath = database.openHelper.writableDatabase.path ?: "."
        val parentDir = File(dbPath).parentFile ?: File(".")
        val tempFile = File(parentDir, "siros_temp_${System.currentTimeMillis()}.csv")

        try {
            Log.d("SirosRepository", "Iniciando syncFromRemote...")
            val downloadResult = remoteSource.downloadCsvToFileInstance(tempFile)
            
            when (downloadResult) {
                is SirosRemoteSource.DownloadResult.Failure -> {
                    Log.e("SirosRepository", "Falha no download: ${downloadResult.errorMessage}")
                    writeSyncFailure(downloadResult.errorMessage, startTime)
                    return@withContext SyncResult.Failure(downloadResult.errorMessage)
                }
                is SirosRemoteSource.DownloadResult.Success -> {
                    var inputStream: FileInputStream? = null
                    var currentError: String? = null
                    var sbcgRecords = mutableListOf<SirosRecord>()

                    try {
                        inputStream = FileInputStream(tempFile)
                        val sequence = SirosStreamParser.parseStream(inputStream)

                        for (record in sequence) {
                            // Filtra SBCG durante o parse sem acumular tudo
                            if (record.codigoOrigem == "SBCG" || record.codigoDestino == "SBCG") {
                                sbcgRecords.add(record)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SirosRepository", "Exceção no parse: ${e.message}")
                        currentError = "Erro no parse: ${e.message}"
                    } finally {
                        inputStream?.close()
                    }

                    if (currentError != null) {
                        Log.e("SirosRepository", "Falha no parse: $currentError")
                        writeSyncFailure(currentError, startTime)
                        return@withContext SyncResult.Failure(currentError)
                    }

                    val normalizedFlights = SirosNormalizer.normalize(sbcgRecords)
                    Log.d("SirosRepository", "Parsed e normalizados ${normalizedFlights.size} voos para SBCG")

                    if (normalizedFlights.isEmpty()) {
                        val errorMsg = "Nenhum voo SBCG encontrado no CSV"
                        Log.w("SirosRepository", errorMsg)
                        writeSyncFailure(errorMsg, startTime)
                        return@withContext SyncResult.Failure(errorMsg)
                    }

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
            writeSyncFailure(e.localizedMessage ?: "Erro desconhecido", startTime)
            SyncResult.Failure(e.localizedMessage ?: "Erro desconhecido")
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun writeSyncFailure(errorMessage: String, startTime: Long = 0) {
        try {
            val metadataDao = database.syncMetadataDao()
            val existing = metadataDao.getSyncMetadata()
            metadataDao.upsertSyncMetadata(
                SyncMetadata(
                    id = 1,
                    lastSuccessfulSyncAt = existing?.lastSuccessfulSyncAt ?: 0L,
                    lastSyncStatus = "FAILURE",
                    lastSyncErrorMessage = errorMessage,
                    recordsInserted = existing?.recordsInserted ?: 0
                )
            )
        } catch (e: Exception) {
            Log.e("SirosRepository", "Falha ao gravar FAILURE: ${e.message}")
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