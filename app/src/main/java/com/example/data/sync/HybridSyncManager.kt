package com.example.data.sync

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.repository.LendingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE_QUEUED,
    ERROR
}

data class HybridSyncUiState(
    val status: SyncStatus = SyncStatus.IDLE,
    val isOnline: Boolean = true,
    val lastSyncTime: Long? = null,
    val pendingChangesCount: Int = 0,
    val statusMessage: String = "",
    val autoSyncEnabled: Boolean = true,
    val userState: GoogleUserState = GoogleUserState()
)

class HybridSyncManager(
    private val context: Context,
    private val repository: LendingRepository,
    private val db: AppDatabase,
    private val authManager: GoogleAuthManager,
    val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sheetsService = GoogleSheetsService()
    private val prefs = context.getSharedPreferences("hybrid_sync_settings", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    private val _lastSyncTime = MutableStateFlow(
        if (prefs.contains("last_sync_time")) prefs.getLong("last_sync_time", 0L) else null
    )
    private val _pendingChangesCount = MutableStateFlow(prefs.getInt("pending_changes", 0))
    private val _statusMessage = MutableStateFlow("Ready to sync")
    private val _autoSyncEnabled = MutableStateFlow(prefs.getBoolean("auto_sync_enabled", true))

    val syncUiState: StateFlow<HybridSyncUiState> = combine(
        combine(_syncStatus, networkMonitor.isOnline, _lastSyncTime) { s, o, l -> Triple(s, o, l) },
        combine(_pendingChangesCount, _statusMessage, _autoSyncEnabled) { p, m, a -> Triple(p, m, a) },
        authManager.userState
    ) { (status, isOnline, lastSync), (pending, msg, autoSync), user ->
        HybridSyncUiState(
            status = status,
            isOnline = isOnline,
            lastSyncTime = lastSync,
            pendingChangesCount = pending,
            statusMessage = msg,
            autoSyncEnabled = autoSync,
            userState = user
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = HybridSyncUiState()
    )

    init {
        // 1. Restore tracked spreadsheet metadata from Room DB if present
        scope.launch {
            try {
                val meta = db.syncMetadataDao().getMetadata()
                if (meta != null && authManager.userState.value.spreadsheetId.isBlank()) {
                    authManager.saveSpreadsheetId(meta.spreadsheetId)
                }
            } catch (_: Exception) {}
        }

        // 2. Observe network state: auto-sync when internet comes back online
        scope.launch {
            var wasOffline = false
            networkMonitor.isOnline.collect { online ->
                if (online && wasOffline) {
                    _statusMessage.value = "Internet restored. Syncing with Google Sheets..."
                    if (_autoSyncEnabled.value && authManager.userState.value.isSignedIn) {
                        syncNow(isManual = false)
                    }
                } else if (!online) {
                    wasOffline = true
                    if (_pendingChangesCount.value > 0) {
                        _syncStatus.value = SyncStatus.OFFLINE_QUEUED
                        _statusMessage.value = "${_pendingChangesCount.value} changes saved locally (offline mode)"
                    }
                }
                if (online) {
                    wasOffline = false
                }
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun onLocalDataMutated() {
        val count = _pendingChangesCount.value + 1
        _pendingChangesCount.value = count
        prefs.edit().putInt("pending_changes", count).apply()

        if (networkMonitor.isCurrentlyOnline() && _autoSyncEnabled.value && authManager.userState.value.isSignedIn) {
            scope.launch {
                syncNow(isManual = false)
            }
        } else if (!networkMonitor.isCurrentlyOnline()) {
            _syncStatus.value = SyncStatus.OFFLINE_QUEUED
            _statusMessage.value = "$count changes saved locally (will sync when online)"
        }
    }

    fun syncNow(isManual: Boolean = true, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        scope.launch {
            val user = authManager.userState.value
            if (!user.isSignedIn) {
                _statusMessage.value = "Please sign in with Google to sync"
                onResult(false, "Please sign in with Google first")
                return@launch
            }

            if (!networkMonitor.isCurrentlyOnline()) {
                _syncStatus.value = SyncStatus.OFFLINE_QUEUED
                _statusMessage.value = "Device is offline. Changes stored safely in local database."
                onResult(false, "Device is offline. Changes stored safely.")
                return@launch
            }

            _syncStatus.value = SyncStatus.SYNCING
            _statusMessage.value = "Connecting to Google Drive & Sheets..."

            try {
                // 1. Obtain a fresh / valid OAuth Access Token
                var token = authManager.getOrRefreshAccessToken(forceRefresh = false)
                if (token.isNullOrBlank()) {
                    token = authManager.getOrRefreshAccessToken(forceRefresh = true)
                }

                if (token.isNullOrBlank()) {
                    _syncStatus.value = SyncStatus.ERROR
                    val errMsg = "Google OAuth authorization required. Please reconnect your Google account in Settings."
                    _statusMessage.value = errMsg
                    onResult(false, errMsg)
                    return@launch
                }

                // 2. Find or create the Google Spreadsheet on user's Google Drive
                // If existing sheet is not found/deleted, a brand-new sheet is automatically created.
                val sheetResult = sheetsService.getOrCreateDatabaseSpreadsheet(
                    accessToken = token,
                    existingId = user.spreadsheetId.ifBlank { null }
                )

                if (sheetResult.isFailure) {
                    val err = sheetResult.exceptionOrNull()?.message ?: "Could not access Google Sheets"
                    _syncStatus.value = SyncStatus.ERROR
                    _statusMessage.value = err
                    onResult(false, err)
                    return@launch
                }

                val sheetId = sheetResult.getOrThrow()
                val sheetUrl = "https://docs.google.com/spreadsheets/d/$sheetId"

                // 3. Persist sheet metadata both in memory/Prefs and in local Room DB table
                authManager.saveSpreadsheetId(sheetId)
                try {
                    db.syncMetadataDao().saveMetadata(
                        SyncMetadataEntity(
                            key = "active_spreadsheet",
                            spreadsheetId = sheetId,
                            spreadsheetUrl = sheetUrl,
                            spreadsheetTitle = GoogleSheetsService.SPREADSHEET_TITLE,
                            userEmail = user.email,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                } catch (_: Exception) {}

                // 4. Fetch local Room DB state
                val people = repository.allPeopleSummaries.first()
                val loans = repository.allLoansWithDetails.first()
                val payments = db.paymentDao().getAllPayments().first()

                // 5. Upload to Google Sheets (People, Loans, Payments)
                val uploadResult = sheetsService.syncAllDataToSheet(
                    accessToken = token,
                    spreadsheetId = sheetId,
                    people = people,
                    loans = loans,
                    payments = payments
                )

                if (uploadResult.isFailure) {
                    val err = uploadResult.exceptionOrNull()?.message ?: "Upload to Google Sheet failed"
                    _syncStatus.value = SyncStatus.ERROR
                    _statusMessage.value = err
                    onResult(false, err)
                    return@launch
                }

                // 6. Update success sync state
                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                _pendingChangesCount.value = 0
                prefs.edit()
                    .putLong("last_sync_time", now)
                    .putInt("pending_changes", 0)
                    .apply()

                _syncStatus.value = SyncStatus.SUCCESS
                _statusMessage.value = "Synced ${people.size} people, ${loans.size} loans with Google Drive ✓"
                onResult(true, "Successfully synced with Google Sheet!")
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                val msg = e.message ?: "Failed to sync"
                _statusMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun restoreFromGoogleSheet(onResult: (Boolean, String) -> Unit) {
        scope.launch {
            val user = authManager.userState.value
            val effectiveSheetId = user.spreadsheetId.ifBlank {
                db.syncMetadataDao().getMetadata()?.spreadsheetId ?: ""
            }

            if (!user.isSignedIn || effectiveSheetId.isBlank()) {
                onResult(false, "No Google Sheet linked yet. Please perform a sync first.")
                return@launch
            }

            if (!networkMonitor.isCurrentlyOnline()) {
                onResult(false, "Internet connection required to download from Google Sheet")
                return@launch
            }

            _syncStatus.value = SyncStatus.SYNCING
            _statusMessage.value = "Downloading data from Google Sheet..."

            var token = authManager.getOrRefreshAccessToken(forceRefresh = false)
            if (token.isNullOrBlank()) {
                token = authManager.getOrRefreshAccessToken(forceRefresh = true)
            }

            if (token.isNullOrBlank()) {
                _syncStatus.value = SyncStatus.ERROR
                val errMsg = "OAuth authorization expired. Please reconnect in Settings."
                _statusMessage.value = errMsg
                onResult(false, errMsg)
                return@launch
            }

            val readResult = sheetsService.readSpreadsheetData(
                accessToken = token,
                spreadsheetId = effectiveSheetId
            )

            readResult.fold(
                onSuccess = { (people, loans, payments) ->
                    withContext(Dispatchers.IO) {
                        // Insert imported records into local Room DB
                        people.forEach { db.personDao().insertPerson(it) }
                        loans.forEach { db.loanDao().insertLoan(it) }
                        payments.forEach { db.paymentDao().insertPayment(it) }
                    }
                    _syncStatus.value = SyncStatus.SUCCESS
                    _statusMessage.value = "Restored ${people.size} people, ${loans.size} loans from Google Sheet"
                    onResult(true, "Restored data from Google Sheet successfully!")
                },
                onFailure = {
                    _syncStatus.value = SyncStatus.ERROR
                    val errMsg = it.message ?: "Download from Google Sheet failed"
                    _statusMessage.value = errMsg
                    onResult(false, errMsg)
                }
            )
        }
    }
}
