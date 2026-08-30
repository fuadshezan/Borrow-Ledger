package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String = "active_spreadsheet"): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE `key` = :key LIMIT 1")
    fun getMetadataFlow(key: String = "active_spreadsheet"): Flow<SyncMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE `key` = :key")
    suspend fun deleteMetadata(key: String = "active_spreadsheet")
}
