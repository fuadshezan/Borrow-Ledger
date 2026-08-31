package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String = "active_spreadsheet",
    val spreadsheetId: String,
    val spreadsheetUrl: String,
    val spreadsheetTitle: String = "Lending Tracker Database",
    val userEmail: String,
    val lastSyncedAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)
