package com.skywatch.cg.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedFlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flight: FollowedFlight): Long

    @Delete
    suspend fun delete(flight: FollowedFlight): Int

    @Query("DELETE FROM followed_flights WHERE flightId = :flightId")
    suspend fun deleteById(flightId: String): Int

    @Query("SELECT * FROM followed_flights ORDER BY flightDate, scheduledDepartureUtc")
    suspend fun getAllFollowedFlights(): List<FollowedFlight>

    @Query("SELECT * FROM followed_flights ORDER BY flightDate, scheduledDepartureUtc")
    fun getAllFollowedFlightsFlow(): Flow<List<FollowedFlight>>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_flights WHERE flightId = :flightId)")
    suspend fun isFollowed(flightId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM followed_flights WHERE flightId = :flightId)")
    fun isFollowedFlow(flightId: String): Flow<Boolean>
}

@Dao
interface AlertSentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertSent): Long

    @Query("SELECT EXISTS(SELECT 1 FROM alert_sent WHERE id = :id)")
    suspend fun isAlertSent(id: String): Boolean

    @Query("DELETE FROM alert_sent WHERE sentAt < :timestamp")
    suspend fun deleteOldAlerts(timestamp: Long): Int
}