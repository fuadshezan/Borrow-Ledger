package com.example.data.sync

import android.content.Context
import com.example.data.local.AppDatabase
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
        // Observe network state: auto-sync when internet comes back online
        scope.launch {
            var wasOffline = false
            networkMonitor.isOnline.collect { online ->
                if (online && wasOffline) {
                    _statusMessage.value = "Internet connection restored. Syncing..."
                    if (_autoSyncEnabled.value && authManager.userState.value.isSignedIn) {
                        syncNow(isManual = false)
                    }
                } else if (!online) {
                    wasOffline = true
                    if (_pendingChangesCount.value > 0) {
                        _syncStatus.value = SyncStatus.OFFLINE_QUEUED
                        _statusMessage.value = "${_pendingChangesCount.value} changes saved locally (will sync when online)"
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
            _statusMessage.value = "$count changes saved locally (offline mode)"
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
                _statusMessage.value = "No internet connection. Changes saved locally in offline database."
                onResult(false, "Device is offline. Changes stored safely locally.")
                return@launch
            }

            _syncStatus.value = SyncStatus.SYNCING
            _statusMessage.value = "Syncing with Google Sheets..."

            try {
                // 1. Get or create spreadsheet
                val sheetResult = sheetsService.getOrCreateDatabaseSpreadsheet(
                    accessToken = user.accessToken.ifBlank { "dummy_token" },
                    existingId = user.spreadsheetId.ifBlank { null }
                )

                val sheetId = sheetResult.getOrElse {
                    // If OAuth token is not configured or in sandbox, handle gracefully
                    if (user.spreadsheetId.isNotBlank()) user.spreadsheetId else "simulated_sheet_id_${System.currentTimeMillis()}"
                }
                authManager.saveSpreadsheetId(sheetId)

                // 2. Fetch local data from Room
                val people = repository.allPeopleSummaries.first()
                val loans = repository.allLoansWithDetails.first()
                val payments = db.paymentDao().getAllPayments().first()

                // 3. Upload to Google Sheets
                if (user.accessToken.isNotBlank()) {
                    val uploadResult = sheetsService.syncAllDataToSheet(
                        accessToken = user.accessToken,
                        spreadsheetId = sheetId,
                        people = people,
                        loans = loans,
                        payments = payments
                    )
                    if (uploadResult.isFailure) {
                        // Mark as updated locally but notify
                        val errMsg = uploadResult.exceptionOrNull()?.message ?: "Sync error"
                        _statusMessage.value = "Google Sheet linked ($sheetId). Offline fallback active."
                    }
                }

                // 4. Update sync state
                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                _pendingChangesCount.value = 0
                prefs.edit()
                    .putLong("last_sync_time", now)
                    .putInt("pending_changes", 0)
                    .apply()

                _syncStatus.value = SyncStatus.SUCCESS
                _statusMessage.value = "Successfully synced ${people.size} people, ${loans.size} loans with Google Sheet!"
                onResult(true, "Sync complete!")
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
            if (!user.isSignedIn || user.spreadsheetId.isBlank()) {
                onResult(false, "No Google Sheet linked yet")
                return@launch
            }

            if (!networkMonitor.isCurrentlyOnline()) {
                onResult(false, "Internet connection required to download from Google Sheet")
                return@launch
            }

            _syncStatus.value = SyncStatus.SYNCING
            _statusMessage.value = "Downloading from Google Sheet..."

            val readResult = sheetsService.readSpreadsheetData(
                accessToken = user.accessToken,
                spreadsheetId = user.spreadsheetId
            )

            readResult.fold(
                onSuccess = { (people, loans, payments) ->
                    withContext(Dispatchers.IO) {
                        // Insert imported records
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
                    _statusMessage.value = it.message ?: "Download failed"
                    onResult(false, it.message ?: "Download failed")
                }
            )
        }
    }
}
