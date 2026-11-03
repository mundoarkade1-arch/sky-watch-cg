package com.skywatch.cg.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_flights")
data class FollowedFlight(
    @PrimaryKey val flightId: String,
    val airline: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val operationType: String,
    val scheduledDepartureUtc: Long,
    val scheduledArrivalUtc: Long,
    val flightDate: String,
    val arrivalDate: String,
    val originTimezone: String,
    val destinationTimezone: String,
    val originName: String?,
    val destinationName: String?,
    val equipment: String?,
    val serviceType: String?,
    val daysOfWeek: String,
    val validFrom: String,
    val validTo: String,
    val sirosStatus: String?,
    val sirosRecordId: String,
    val createdAt: Long
)

@Entity(tableName = "alert_sent")
data class AlertSent(
    @PrimaryKey val id: String,
    val flightId: String,
    val operationType: String,
    val eventDate: String,
    val minutesBefore: Int,
    val sentAt: Long
)