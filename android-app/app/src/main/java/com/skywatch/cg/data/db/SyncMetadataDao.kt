package com.skywatch.cg.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    suspend fun getSyncMetadata(): SyncMetadata?

    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    fun getSyncMetadataFlow(): Flow<SyncMetadata?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMetadata(metadata: SyncMetadata): Long
}