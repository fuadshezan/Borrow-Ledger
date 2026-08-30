package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SeedData
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.model.ActivityItem
import com.example.data.model.DashboardSummary
import com.example.data.model.LoanDirection
import com.example.data.model.LoanStatus
import com.example.data.model.LoanWithDetails
import com.example.data.model.PaymentItem
import com.example.data.model.PersonSummary
import com.example.data.model.ReminderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar

class LendingRepository(private val db: AppDatabase) {

    private val personDao = db.personDao()
    private val loanDao = db.loanDao()
    private val paymentDao = db.paymentDao()
    private val reminderDao = db.reminderDao()

    init {
        // We will seed if empty in ViewModel or startup
    }

    suspend fun seedDatabaseIfEmpty() {
        SeedData.populateIfEmpty(db)
    }

    // 1. Loans with detailed status & calculations
    val allLoansWithDetails: Flow<List<LoanWithDetails>> = combine(
        personDao.getAllPeople(),
        loanDao.getAllLoans(),
        paymentDao.getAllPayments()
    ) { people, loans, payments ->
        val personMap = people.associateBy { it.id }
        val paymentsByLoan = payments.groupBy { it.loanId }
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        val sevenDaysFromNow = todayStart + (7L * 24 * 60 * 60 * 1000L)

        loans.map { loan ->
            val person = personMap[loan.personId]
            val loanPayments = paymentsByLoan[loan.id]?.map { p ->
                PaymentItem(
                    id = p.id,
                    loanId = p.loanId,
                    amount = p.amount,
                    paymentDate = p.paymentDate,
                    paymentMethod = p.paymentMethod,
                    note = p.note,
                    proofUri = p.proofUri,
                    createdAt = p.createdAt
                )
            } ?: emptyList()

            val totalPaid = loanPayments.sumOf { it.amount }
            val outstanding = maxOf(0.0, loan.originalAmount - totalPaid)
            val isSettled = outstanding <= 0.001

            val status: LoanStatus = when {
                isSettled -> LoanStatus.SETTLED
                loan.dueDate == null -> LoanStatus.NO_DUE_DATE
                loan.dueDate < todayStart -> LoanStatus.OVERDUE
                loan.dueDate <= sevenDaysFromNow -> LoanStatus.DUE_SOON
                else -> LoanStatus.ACTIVE
            }

            val daysOverdue = if (!isSettled && loan.dueDate != null && loan.dueDate < todayStart) {
                ((todayStart - loan.dueDate) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            } else 0

            val daysUntilDue = if (!isSettled && loan.dueDate != null && loan.dueDate >= todayStart) {
                ((loan.dueDate - todayStart) / (1000 * 60 * 60 * 24)).toInt()
            } else null

            LoanWithDetails(
                id = loan.id,
                personId = loan.personId,
                personName = person?.name ?: "Unknown Person",
                personPhone = person?.phone ?: "",
                direction = if (loan.direction == "BORROWED") LoanDirection.BORROWED else LoanDirection.LENT,
                originalAmount = loan.originalAmount,
                currency = loan.currency,
                loanDate = loan.loanDate,
                dueDate = loan.dueDate,
                purpose = loan.purpose,
                note = loan.note,
                paymentMethod = loan.paymentMethod,
                installmentAmount = loan.installmentAmount,
                installmentFrequency = loan.installmentFrequency,
                installmentCount = loan.installmentCount,
                payments = loanPayments,
                totalPaid = totalPaid,
                outstanding = outstanding,
                isSettled = isSettled,
                status = status,
                daysOverdue = daysOverdue,
                daysUntilDue = daysUntilDue,
                createdAt = loan.createdAt,
                updatedAt = loan.updatedAt
            )
        }
    }.flowOn(Dispatchers.Default)

    // 2. People summaries aggregating multiple loans
    val allPeopleSummaries: Flow<List<PersonSummary>> = combine(
        personDao.getAllPeople(),
        allLoansWithDetails
    ) { people, loansWithDetails ->
        val loansByPerson = loansWithDetails.groupBy { it.personId }

        people.map { person ->
            val personLoans = loansByPerson[person.id] ?: emptyList()
            val lentLoans = personLoans.filter { it.direction == LoanDirection.LENT }
            val borrowedLoans = personLoans.filter { it.direction == LoanDirection.BORROWED }

            val totalLent = lentLoans.sumOf { it.originalAmount }
            val totalLentReturned = lentLoans.sumOf { it.totalPaid }
            val totalLentOutstanding = lentLoans.sumOf { it.outstanding }

            val totalBorrowed = borrowedLoans.sumOf { it.originalAmount }
            val totalBorrowedReturned = borrowedLoans.sumOf { it.totalPaid }
            val totalBorrowedOutstanding = borrowedLoans.sumOf { it.outstanding }

            val netBalance = totalLentOutstanding - totalBorrowedOutstanding
            val activeLoans = personLoans.filter { !it.isSettled }
            val settledLoans = personLoans.filter { it.isSettled }

            val hasOverdue = activeLoans.any { it.status == LoanStatus.OVERDUE }
            val hasDueSoon = activeLoans.any { it.status == LoanStatus.DUE_SOON }

            val earliestDue = activeLoans
                .mapNotNull { it.dueDate }
                .minOrNull()

            PersonSummary(
                id = person.id,
                name = person.name,
                phone = person.phone,
                email = person.email,
                notes = person.notes,
                totalLent = totalLent,
                totalLentReturned = totalLentReturned,
                totalLentOutstanding = totalLentOutstanding,
                totalBorrowed = totalBorrowed,
                totalBorrowedReturned = totalBorrowedReturned,
                totalBorrowedOutstanding = totalBorrowedOutstanding,
                netBalanceOwedToUser = netBalance,
                activeLoansCount = activeLoans.size,
                settledLoansCount = settledLoans.size,
                hasOverdue = hasOverdue,
                hasDueSoon = hasDueSoon,
                earliestDueDate = earliestDue,
                isArchived = person.isArchived,
                createdAt = person.createdAt
            )
        }
    }.flowOn(Dispatchers.Default)

    // 3. Who Owes Me (Debtors)
    val whoOwesMeList: Flow<List<PersonSummary>> = allPeopleSummaries.combine(allLoansWithDetails) { summaries, _ ->
        summaries.filter { it.totalLentOutstanding > 0.001 }
            .sortedByDescending { it.totalLentOutstanding }
    }.flowOn(Dispatchers.Default)

    // 4. Dashboard Summary
    val dashboardSummary: Flow<DashboardSummary> = allLoansWithDetails.combine(allPeopleSummaries) { loans, people ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        var peopleOweMe = 0.0
        var iOweOthers = 0.0
        var overdue = 0.0
        var dueSoon = 0.0
        var activeLoansCount = 0
        var settledLoansCount = 0
        var lifetimeLent = 0.0
        var lifetimeReturned = 0.0

        var monthLent = 0.0
        var monthReturned = 0.0
        var monthNewLoans = 0
        var monthRepayments = 0

        loans.forEach { loan ->
            if (loan.direction == LoanDirection.LENT) {
                peopleOweMe += loan.outstanding
                lifetimeLent += loan.originalAmount
                lifetimeReturned += loan.totalPaid

                if (isSameMonth(loan.loanDate, currentMonth, currentYear)) {
                    monthLent += loan.originalAmount
                    monthNewLoans++
                }
            } else {
                iOweOthers += loan.outstanding
            }

            if (loan.isSettled) {
                settledLoansCount++
            } else {
                activeLoansCount++
                if (loan.status == LoanStatus.OVERDUE) {
                    overdue += loan.outstanding
                } else if (loan.status == LoanStatus.DUE_SOON) {
                    dueSoon += loan.outstanding
                }
            }

            loan.payments.forEach { payment ->
                if (isSameMonth(payment.paymentDate, currentMonth, currentYear)) {
                    if (loan.direction == LoanDirection.LENT) {
                        monthReturned += payment.amount
                    }
                    monthRepayments++
                }
            }
        }

        val activeDebtorsCount = people.count { it.totalLentOutstanding > 0.001 }

        DashboardSummary(
            peopleOweMeTotal = peopleOweMe,
            iOweOthersTotal = iOweOthers,
            overdueTotal = overdue,
            dueSoonTotal = dueSoon,
            activeDebtorsCount = activeDebtorsCount,
            activeLoansCount = activeLoansCount,
            settledLoansCount = settledLoansCount,
            thisMonthLent = monthLent,
            thisMonthReturned = monthReturned,
            thisMonthNewLoansCount = monthNewLoans,
            thisMonthRepaymentsCount = monthRepayments,
            lifetimeLent = lifetimeLent,
            lifetimeReturned = lifetimeReturned
        )
    }.flowOn(Dispatchers.Default)

    // 5. Recent Activity
    val recentActivity: Flow<List<ActivityItem>> = combine(
        allLoansWithDetails,
        paymentDao.getAllPayments()
    ) { loans, _ ->
        val activities = mutableListOf<ActivityItem>()

        loans.forEach { loan ->
            activities.add(
                ActivityItem(
                    id = "loan_${loan.id}",
                    title = if (loan.direction == LoanDirection.LENT) "You lent ${loan.personName}" else "${loan.personName} lent you",
                    subtitle = if (loan.purpose.isNotBlank()) loan.purpose else "Loan created",
                    amount = loan.originalAmount,
                    date = loan.loanDate,
                    isPayment = false,
                    direction = loan.direction,
                    loanId = loan.id,
                    personName = loan.personName,
                    paymentMethod = loan.paymentMethod
                )
            )

            loan.payments.forEach { payment ->
                activities.add(
                    ActivityItem(
                        id = "payment_${payment.id}",
                        title = if (loan.direction == LoanDirection.LENT) "${loan.personName} returned" else "You repaid ${loan.personName}",
                        subtitle = if (payment.note.isNotBlank()) payment.note else "${payment.paymentMethod} payment",
                        amount = payment.amount,
                        date = payment.paymentDate,
                        isPayment = true,
                        direction = loan.direction,
                        loanId = loan.id,
                        personName = loan.personName,
                        paymentMethod = payment.paymentMethod
                    )
                )
            }
        }

        activities.sortedByDescending { it.date }
    }.flowOn(Dispatchers.Default)

    // 6. Reminders List
    val reminderItems: Flow<List<ReminderItem>> = combine(
        allLoansWithDetails,
        reminderDao.getAllReminders()
    ) { loans, reminders ->
        val loanMap = loans.associateBy { it.id }
        val items = mutableListOf<ReminderItem>()

        reminders.forEach { r ->
            val loan = loanMap[r.loanId]
            if (loan != null && !loan.isSettled) {
                items.add(
                    ReminderItem(
                        id = r.id,
                        loanId = r.loanId,
                        personName = loan.personName,
                        personPhone = loan.personPhone,
                        loanPurpose = loan.purpose,
                        outstandingAmount = loan.outstanding,
                        dueDate = loan.dueDate,
                        status = loan.status,
                        message = r.notes.ifBlank {
                            if (loan.status == LoanStatus.OVERDUE) "${loan.personName}'s repayment is overdue!"
                            else "${loan.personName}'s repayment is due soon."
                        },
                        reminderType = r.reminderType,
                        reminderDate = r.reminderDate,
                        isCompleted = r.isCompleted
                    )
                )
            }
        }

        // Auto-generate items for overdue & due soon loans if not already explicitly in reminder table
        loans.filter { !it.isSettled && (it.status == LoanStatus.OVERDUE || it.status == LoanStatus.DUE_SOON) }
            .forEach { loan ->
                if (items.none { it.loanId == loan.id }) {
                    items.add(
                        ReminderItem(
                            id = -(loan.id), // dynamic ID
                            loanId = loan.id,
                            personName = loan.personName,
                            personPhone = loan.personPhone,
                            loanPurpose = loan.purpose,
                            outstandingAmount = loan.outstanding,
                            dueDate = loan.dueDate,
                            status = loan.status,
                            message = if (loan.status == LoanStatus.OVERDUE) {
                                "${loan.personName}'s repayment of ৳${loan.outstanding.toInt()} is overdue by ${loan.daysOverdue} days."
                            } else {
                                "${loan.personName}'s repayment is due in ${loan.daysUntilDue ?: 0} days."
                            },
                            reminderType = if (loan.status == LoanStatus.OVERDUE) "OVERDUE" else "UPCOMING",
                            reminderDate = loan.dueDate ?: System.currentTimeMillis(),
                            isCompleted = false
                        )
                    )
                }
            }

        items.sortedWith(
            compareBy<ReminderItem> { it.isCompleted }
                .thenBy { it.status != LoanStatus.OVERDUE }
                .thenBy { it.reminderDate }
        )
    }.flowOn(Dispatchers.Default)

    // Helper month check
    private fun isSameMonth(timestamp: Long, month: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
    }

    // --- Action Methods ---

    suspend fun getLoanDetailsById(loanId: Long): LoanWithDetails? = withContext(Dispatchers.IO) {
        allLoansWithDetails.first().find { it.id == loanId }
    }

    suspend fun getPersonSummaryById(personId: Long): PersonSummary? = withContext(Dispatchers.IO) {
        allPeopleSummaries.first().find { it.id == personId }
    }

    suspend fun addPerson(name: String, phone: String, email: String, notes: String): Long = withContext(Dispatchers.IO) {
        personDao.insertPerson(
            PersonEntity(
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                notes = notes.trim()
            )
        )
    }

    suspend fun updatePerson(person: PersonEntity) = withContext(Dispatchers.IO) {
        personDao.updatePerson(person.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePerson(personId: Long) = withContext(Dispatchers.IO) {
        val person = personDao.getPersonByIdSync(personId)
        if (person != null) {
            personDao.deletePerson(person)
        }
    }

    suspend fun addLoan(
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
        installmentCount: Int? = null
    ): Long = withContext(Dispatchers.IO) {
        val loanId = loanDao.insertLoan(
            LoanEntity(
                personId = personId,
                direction = direction.name,
                originalAmount = amount,
                loanDate = loanDate,
                dueDate = dueDate,
                purpose = purpose.trim(),
                note = note.trim(),
                paymentMethod = paymentMethod,
                installmentAmount = installmentAmount,
                installmentFrequency = installmentFrequency,
                installmentCount = installmentCount
            )
        )

        // Create default reminder if due date is specified
        if (dueDate != null && dueDate > System.currentTimeMillis()) {
            val sevenDaysBefore = dueDate - (7L * 24 * 60 * 60 * 1000L)
            reminderDao.insertReminder(
                ReminderEntity(
                    loanId = loanId,
                    reminderType = "UPCOMING_7D",
                    reminderDate = if (sevenDaysBefore > System.currentTimeMillis()) sevenDaysBefore else dueDate,
                    isCompleted = false,
                    notes = "Repayment due on ${dueDate}"
                )
            )
        }
        loanId
    }

    suspend fun updateLoan(loan: LoanEntity) = withContext(Dispatchers.IO) {
        loanDao.updateLoan(loan.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteLoan(loanId: Long) = withContext(Dispatchers.IO) {
        loanDao.deleteLoanById(loanId)
    }

    suspend fun addPayment(
        loanId: Long,
        amount: Double,
        paymentDate: Long,
        paymentMethod: String,
        note: String,
        proofUri: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanByIdSync(loanId) ?: return@withContext Result.failure(Exception("Loan not found"))
        val currentPayments = paymentDao.getPaymentsForLoan(loanId).first()
        val totalPaid = currentPayments.sumOf { it.amount }
        val outstanding = loan.originalAmount - totalPaid

        // Strict overpayment validation rule: MVP checks overpayment
        if (amount > outstanding + 0.001) {
            return@withContext Result.failure(
                IllegalArgumentException("Amount (৳${amount}) exceeds remaining outstanding balance of ৳${outstanding.toInt()}")
            )
        }

        val paymentId = paymentDao.insertPayment(
            PaymentEntity(
                loanId = loanId,
                amount = amount,
                paymentDate = paymentDate,
                paymentMethod = paymentMethod,
                note = note.trim(),
                proofUri = proofUri
            )
        )
        Result.success(paymentId)
    }

    suspend fun updatePayment(payment: PaymentEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanByIdSync(payment.loanId) ?: return@withContext Result.failure(Exception("Loan not found"))
        val currentPayments = paymentDao.getPaymentsForLoan(payment.loanId).first()
        val totalOtherPayments = currentPayments.filter { it.id != payment.id }.sumOf { it.amount }
        val remainingCapacity = loan.originalAmount - totalOtherPayments

        if (payment.amount > remainingCapacity + 0.001) {
            return@withContext Result.failure(
                IllegalArgumentException("Updated payment exceeds remaining capacity of ৳${remainingCapacity.toInt()}")
            )
        }

        paymentDao.updatePayment(payment.copy(updatedAt = System.currentTimeMillis()))
        Result.success(Unit)
    }

    suspend fun deletePayment(paymentId: Long) = withContext(Dispatchers.IO) {
        paymentDao.deletePaymentById(paymentId)
    }

    suspend fun toggleReminderCompleted(reminderId: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        if (reminderId > 0) {
            reminderDao.setCompleted(reminderId, isCompleted)
        }
    }

    // Quick Add: Superfast 5-second flow
    suspend fun quickAddTransaction(
        personName: String,
        isGaveMoney: Boolean,
        amount: Double,
        date: Long,
        paymentMethod: String = "bKash",
        purpose: String = "Quick Loan",
        note: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        val trimmedName = personName.trim()
        val allPeople = personDao.getAllPeople().first()
        val existingPerson = allPeople.find { it.name.equals(trimmedName, ignoreCase = true) }
        val personId = existingPerson?.id ?: personDao.insertPerson(
            PersonEntity(name = trimmedName)
        )

        val direction = if (isGaveMoney) LoanDirection.LENT else LoanDirection.BORROWED
        val loanId = loanDao.insertLoan(
            LoanEntity(
                personId = personId,
                direction = direction.name,
                originalAmount = amount,
                loanDate = date,
                dueDate = null,
                purpose = purpose,
                note = note,
                paymentMethod = paymentMethod
            )
        )
        Result.success(loanId)
    }

    suspend fun resetWithDemoData() = withContext(Dispatchers.IO) {
        // Clear all
        val people = personDao.getAllPeople().first()
        people.forEach { personDao.deletePerson(it) }
        SeedData.populateIfEmpty(db)
    }
}
