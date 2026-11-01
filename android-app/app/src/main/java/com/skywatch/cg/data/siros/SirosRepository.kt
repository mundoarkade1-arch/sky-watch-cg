package com.skywatch.cg.data.siros

import android.util.Log
import com.skywatch.cg.data.db.AppDatabase
import com.skywatch.cg.data.db.FlightEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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