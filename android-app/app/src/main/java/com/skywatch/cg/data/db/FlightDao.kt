package com.skywatch.cg.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flights: List<FlightEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flight: FlightEntity): Long

    @Update
    suspend fun update(flight: FlightEntity): Int

    @Delete
    suspend fun delete(flight: FlightEntity): Int

    @Query("SELECT * FROM flights WHERE (operationType = 'ARRIVAL' AND arrivalDate = :date) OR (operationType = 'DEPARTURE' AND flightDate = :date) ORDER BY operationType, scheduledDepartureUtc")
    suspend fun getFlightsByDate(date: String): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE (operationType = 'ARRIVAL' AND arrivalDate = :date) OR (operationType = 'DEPARTURE' AND flightDate = :date) ORDER BY operationType, scheduledDepartureUtc")
    fun getFlightsByDateFlow(date: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE operationType = 'ARRIVAL' AND arrivalDate = :date ORDER BY scheduledArrivalUtc")
    suspend fun getArrivalsByDate(date: String): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE operationType = 'ARRIVAL' AND arrivalDate = :date ORDER BY scheduledArrivalUtc")
    fun getArrivalsByDateFlow(date: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE operationType = 'ARRIVAL' AND arrivalDate BETWEEN :startDate AND :endDate ORDER BY arrivalDate, scheduledArrivalUtc")
    suspend fun getArrivalsByDateRange(startDate: String, endDate: String): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE operationType = 'ARRIVAL' AND arrivalDate BETWEEN :startDate AND :endDate ORDER BY arrivalDate, scheduledArrivalUtc")
    fun getArrivalsByDateRangeFlow(startDate: String, endDate: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE operationType = 'DEPARTURE' AND flightDate = :date ORDER BY scheduledDepartureUtc")
    suspend fun getDeparturesByDate(date: String): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE operationType = 'DEPARTURE' AND flightDate = :date ORDER BY scheduledDepartureUtc")
    fun getDeparturesByDateFlow(date: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE operationType = 'DEPARTURE' AND flightDate BETWEEN :startDate AND :endDate ORDER BY flightDate, scheduledDepartureUtc")
    suspend fun getDeparturesByDateRange(startDate: String, endDate: String): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE operationType = 'DEPARTURE' AND flightDate BETWEEN :startDate AND :endDate ORDER BY flightDate, scheduledDepartureUtc")
    fun getDeparturesByDateRangeFlow(startDate: String, endDate: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE operationType = 'ARRIVAL' ORDER BY arrivalDate, scheduledArrivalUtc")
    suspend fun getAllArrivals(): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE operationType = 'ARRIVAL' ORDER BY arrivalDate, scheduledArrivalUtc")
    fun getAllArrivalsFlow(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE operationType = 'DEPARTURE' ORDER BY flightDate, scheduledDepartureUtc")
    suspend fun getAllDepartures(): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE operationType = 'DEPARTURE' ORDER BY flightDate, scheduledDepartureUtc")
    fun getAllDeparturesFlow(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE sirosRecordId = :recordId")
    suspend fun getBySirosRecordId(recordId: String): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE flightId = :flightId")
    suspend fun getByFlightId(flightId: String): FlightEntity?

    @Query("SELECT * FROM flights WHERE flightDate < :beforeDate")
    suspend fun getOldFlights(beforeDate: String): List<FlightEntity>

    @Query("DELETE FROM flights WHERE flightDate < :beforeDate")
    suspend fun deleteOldFlights(beforeDate: String): Int

    @Query("DELETE FROM flights")
    suspend fun deleteAll(): Int

    @Transaction
    suspend fun upsertAll(flights: List<FlightEntity>) {
        for (flight in flights) {
            insert(flight)
        }
    }
}