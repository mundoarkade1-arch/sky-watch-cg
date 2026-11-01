package com.skywatch.cg.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey
    val id: Int = 1,
    val lastSuccessfulSyncAt: Long = 0L,
    val lastSyncStatus: String = "IDLE", // IDLE, SUCCESS, ERROR
    val lastSyncErrorMessage: String? = null,
    val recordsInserted: Int = 0
)
