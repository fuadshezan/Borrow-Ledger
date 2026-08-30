package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.ActivityItem
import com.example.data.model.AppThemeMode
import com.example.data.model.DashboardSummary
import com.example.data.model.LoanDirection
import com.example.data.model.LoanStatus
import com.example.data.model.LoanWithDetails
import com.example.data.model.PersonSummary
import com.example.data.model.ReminderItem
import com.example.data.repository.LendingRepository
import com.example.data.sync.GoogleAuthManager
import com.example.data.sync.GoogleUserState
import com.example.data.sync.HybridSyncManager
import com.example.data.sync.HybridSyncUiState
import com.example.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LoanFilterOption(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    DUE_SOON("Due Soon"),
    OVERDUE("Overdue"),
    NO_DUE_DATE("No Due Date"),
    SETTLED("Settled")
}

class LendingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = LendingRepository(db)

    val authManager = GoogleAuthManager(application)
    val networkMonitor = NetworkMonitor(application)
    val syncManager = HybridSyncManager(application, repository, db, authManager, networkMonitor)

    val syncUiState: StateFlow<HybridSyncUiState> = syncManager.syncUiState
    val googleUserState: StateFlow<GoogleUserState> = authManager.userState

    private val prefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _currencySymbol = MutableStateFlow(
        prefs.getString("currency_symbol", "৳") ?: "৳"
    )
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    // Filters
    private val _loanStatusFilter = MutableStateFlow(LoanFilterOption.ALL)
    val loanStatusFilter: StateFlow<LoanFilterOption> = _loanStatusFilter.asStateFlow()

    private val _directionFilter = MutableStateFlow<LoanDirection?>(null)
    val directionFilter: StateFlow<LoanDirection?> = _directionFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.seedDatabaseIfEmpty()
            } catch (e: Exception) {
                // Ignore or log gracefully
            }
        }
    }

    // Dashboard
    val dashboardSummary: StateFlow<DashboardSummary> = repository.dashboardSummary
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardSummary(
                0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0.0, 0.0, 0, 0, 0.0, 0.0
            )
        )

    // All Loans Raw
    val allLoans: StateFlow<List<LoanWithDetails>> = repository.allLoansWithDetails
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered Loans
    val filteredLoans: StateFlow<List<LoanWithDetails>> = combine(
        repository.allLoansWithDetails,
        _loanStatusFilter,
        _directionFilter
    ) { loans, statusFilter, direction ->
        loans.filter { loan ->
            val statusMatch = when (statusFilter) {
                LoanFilterOption.ALL -> true
                LoanFilterOption.ACTIVE -> loan.status == LoanStatus.ACTIVE
                LoanFilterOption.DUE_SOON -> loan.status == LoanStatus.DUE_SOON
                LoanFilterOption.OVERDUE -> loan.status == LoanStatus.OVERDUE
                LoanFilterOption.NO_DUE_DATE -> loan.status == LoanStatus.NO_DUE_DATE
                LoanFilterOption.SETTLED -> loan.status == LoanStatus.SETTLED
            }
            val directionMatch = direction == null || loan.direction == direction
            statusMatch && directionMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // People
    val allPeople: StateFlow<List<PersonSummary>> = repository.allPeopleSummaries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Who Owes Me
    val whoOwesMeList: StateFlow<List<PersonSummary>> = repository.whoOwesMeList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recent Activity
    val recentActivity: StateFlow<List<ActivityItem>> = repository.recentActivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reminders
    val reminderItems: StateFlow<List<ReminderItem>> = repository.reminderItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Global Search Results
    data class SearchResults(
        val people: List<PersonSummary> = emptyList(),
        val loans: List<LoanWithDetails> = emptyList(),
        val payments: List<ActivityItem> = emptyList()
    )

    val searchResults: StateFlow<SearchResults> = combine(
        _searchQuery,
        repository.allPeopleSummaries,
        repository.allLoansWithDetails,
        repository.recentActivity
    ) { query, people, loans, activities ->
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            SearchResults()
        } else {
            val matchedPeople = people.filter {
                it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) || it.notes.lowercase().contains(q)
            }
            val matchedLoans = loans.filter {
                it.personName.lowercase().contains(q) ||
                        it.purpose.lowercase().contains(q) ||
                        it.note.lowercase().contains(q) ||
                        it.originalAmount.toString().contains(q) ||
                        it.paymentMethod.lowercase().contains(q) ||
                        "l${it.id}".contains(q)
            }
            val matchedPayments = activities.filter {
                it.isPayment && (
                        it.personName.lowercase().contains(q) ||
                                it.subtitle.lowercase().contains(q) ||
                                it.amount.toString().contains(q) ||
                                it.paymentMethod.lowercase().contains(q)
                        )
            }
            SearchResults(matchedPeople, matchedLoans, matchedPayments)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchResults()
    )

    // Filter Controls
    fun setLoanStatusFilter(filter: LoanFilterOption) {
        _loanStatusFilter.value = filter
    }

    fun setDirectionFilter(direction: LoanDirection?) {
        _directionFilter.value = direction
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun toggleThemeMode() {
        val next = when (_themeMode.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
        }
        setThemeMode(next)
    }

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    // Actions
    fun addPerson(name: String, phone: String, email: String, notes: String, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addPerson(name, phone, email, notes)
            _snackbarEvent.emit("Person '$name' added successfully")
            syncManager.onLocalDataMutated()
            onComplete(id)
        }
    }

    fun deletePerson(personId: Long, personName: String) {
        viewModelScope.launch {
            repository.deletePerson(personId)
            _snackbarEvent.emit("Deleted $personName and all related loans")
            syncManager.onLocalDataMutated()
        }
    }

    fun addLoan(
        personId: Long,
        direction: LoanDirection,
        amount: Double,
        loanDate: Long,
        dueDate: Long?,
        purpose: String,
        note: String,
        paymentMethod: String,
        installmentAmount: Double? = null,
        installmentFrequency: String? = null,
        installmentCount: Int? = null,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.addLoan(
                personId = personId,
                direction = direction,
                amount = amount,
                loanDate = loanDate,
                dueDate = dueDate,
                purpose = purpose,
                note = note,
                paymentMethod = paymentMethod,
                installmentAmount = installmentAmount,
                installmentFrequency = installmentFrequency,
                installmentCount = installmentCount
            )
            val action = if (direction == LoanDirection.LENT) "Lent" else "Borrowed"
            _snackbarEvent.emit("Recorded: $action ${currencySymbol.value}${amount.toInt()}")
            syncManager.onLocalDataMutated()
            onComplete(id)
        }
    }

    fun deleteLoan(loanId: Long, loanName: String) {
        viewModelScope.launch {
            repository.deleteLoan(loanId)
            _snackbarEvent.emit("Deleted loan: $loanName")
            syncManager.onLocalDataMutated()
        }
    }

    fun recordPayment(
        loanId: Long,
        amount: Double,
        paymentDate: Long,
        paymentMethod: String,
        note: String,
        proofUri: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.addPayment(
                loanId = loanId,
                amount = amount,
                paymentDate = paymentDate,
                paymentMethod = paymentMethod,
                note = note,
                proofUri = proofUri
            )
            result.fold(
                onSuccess = {
                    _snackbarEvent.emit("Payment of ${currencySymbol.value}${amount.toInt()} recorded ✓")
                    syncManager.onLocalDataMutated()
                    onSuccess()
                },
                onFailure = { error ->
                    val msg = error.message ?: "Could not record payment"
                    _snackbarEvent.emit(msg)
                    onError(msg)
                }
            )
        }
    }

    fun deletePayment(paymentId: Long, amount: Double) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
            _snackbarEvent.emit("Payment of ${currencySymbol.value}${amount.toInt()} deleted. Balance restored.")
            syncManager.onLocalDataMutated()
        }
    }

    fun quickAdd(
        personName: String,
        isGaveMoney: Boolean,
        amount: Double,
        date: Long,
        paymentMethod: String,
        purpose: String,
        note: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.quickAddTransaction(
                personName = personName,
                isGaveMoney = isGaveMoney,
                amount = amount,
                date = date,
                paymentMethod = paymentMethod,
                purpose = purpose,
                note = note
            )
            result.fold(
                onSuccess = {
                    val relationship = if (isGaveMoney) "$personName owes you" else "You owe $personName"
                    _snackbarEvent.emit("✓ Recorded! $relationship ${currencySymbol.value}${amount.toInt()}")
                    syncManager.onLocalDataMutated()
                    onComplete()
                },
                onFailure = {
                    _snackbarEvent.emit(it.message ?: "Failed to record quick transaction")
                }
            )
        }
    }

    fun toggleReminder(reminderId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleReminderCompleted(reminderId, isCompleted)
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetWithDemoData()
            _snackbarEvent.emit("Reset sample records successfully")
            syncManager.onLocalDataMutated()
        }
    }

    // Google Account & Sheets Sync Controls

    /**
     * Processes the Intent result from the Google Sign-In activity.
     * Delegates to [GoogleAuthManager.handleSignInResult], which signs into Firebase
     * and acquires an OAuth access token with Sheets/Drive scopes.
     *
     * On success the sync pipeline is immediately triggered so the Google Sheet
     * is created/updated right away.
     */
    fun handleGoogleSignInResult(data: Intent?) {
        if (data == null) return // User cancelled the picker
        viewModelScope.launch {
            val result = authManager.handleSignInResult(data)
            result.fold(
                onSuccess = { userState ->
                    _snackbarEvent.emit("✓ Signed in as ${userState.email}")
                    syncManager.syncNow(isManual = true)
                },
                onFailure = { error ->
                    val msg = error.message ?: "Sign-in failed"
                    // Don't show an error snackbar for a user-initiated cancellation
                    if (!msg.contains("cancelled", ignoreCase = true)) {
                        _snackbarEvent.emit("Sign-in failed: $msg")
                    }
                }
            )
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            authManager.signOut()
            try {
                db.syncMetadataDao().deleteMetadata()
            } catch (_: Exception) {}
            _snackbarEvent.emit("Signed out of Google account")
        }
    }

    fun syncNow() {
        syncManager.syncNow(isManual = true) { success, msg ->
            viewModelScope.launch {
                _snackbarEvent.emit(msg)
            }
        }
    }

    fun restoreFromGoogleSheet() {
        syncManager.restoreFromGoogleSheet { success, msg ->
            viewModelScope.launch {
                _snackbarEvent.emit(msg)
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        syncManager.setAutoSyncEnabled(enabled)
    }

    fun saveCustomSpreadsheetId(id: String) {
        authManager.saveSpreadsheetId(id)
        viewModelScope.launch {
            _snackbarEvent.emit("Linked Google Spreadsheet ID: $id")
        }
    }
}
