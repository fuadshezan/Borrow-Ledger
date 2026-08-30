package com.example.data.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LoanDirection(val label: String, val shortDesc: String) {
    LENT("I Lent Money", "Owes you"),
    BORROWED("I Borrowed Money", "You owe")
}

enum class LoanStatus(val label: String, val colorCode: Long) {
    ACTIVE("Active", 0xFF3B82F6),        // Blue
    DUE_SOON("Due Soon", 0xFFF59E0B),    // Amber
    OVERDUE("Overdue", 0xFFEF4444),      // Red
    SETTLED("Settled", 0xFF10B981),      // Green
    NO_DUE_DATE("No Due Date", 0xFF64748B) // Slate
}

enum class PaymentMethodOption(val displayName: String) {
    BKASH("bKash"),
    CASH("Cash"),
    NAGAD("Nagad"),
    ROCKET("Rocket"),
    BANK_TRANSFER("Bank Transfer"),
    CARD("Card"),
    OTHER("Other")
}

enum class LoanPurposeCategory(val title: String) {
    EMERGENCY("Emergency"),
    PERSONAL("Personal"),
    LAPTOP_GADGET("Laptop / Gadget"),
    TRAVEL("Travel"),
    MEDICAL("Medical"),
    BUSINESS("Business"),
    EDUCATION("Education"),
    RENT("Rent"),
    OTHER("Other")
}

data class LoanWithDetails(
    val id: Long,
    val personId: Long,
    val personName: String,
    val personPhone: String,
    val direction: LoanDirection,
    val originalAmount: Double,
    val currency: String = "BDT",
    val loanDate: Long,
    val dueDate: Long?,
    val purpose: String,
    val note: String,
    val paymentMethod: String,
    val installmentAmount: Double?,
    val installmentFrequency: String?,
    val installmentCount: Int?,
    val payments: List<PaymentItem> = emptyList(),
    val totalPaid: Double,
    val outstanding: Double,
    val isSettled: Boolean,
    val status: LoanStatus,
    val daysOverdue: Int = 0,
    val daysUntilDue: Int? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class PaymentItem(
    val id: Long,
    val loanId: Long,
    val amount: Double,
    val paymentDate: Long,
    val paymentMethod: String,
    val note: String,
    val proofUri: String?,
    val createdAt: Long
)

data class PersonSummary(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String,
    val notes: String,
    val totalLent: Double,
    val totalLentReturned: Double,
    val totalLentOutstanding: Double,
    val totalBorrowed: Double,
    val totalBorrowedReturned: Double,
    val totalBorrowedOutstanding: Double,
    val netBalanceOwedToUser: Double, // Lent outstanding - Borrowed outstanding
    val activeLoansCount: Int,
    val settledLoansCount: Int,
    val hasOverdue: Boolean,
    val hasDueSoon: Boolean,
    val earliestDueDate: Long?,
    val isArchived: Boolean,
    val createdAt: Long
)

data class DashboardSummary(
    val peopleOweMeTotal: Double,
    val iOweOthersTotal: Double,
    val overdueTotal: Double,
    val dueSoonTotal: Double,
    val activeDebtorsCount: Int,
    val activeLoansCount: Int,
    val settledLoansCount: Int,
    val thisMonthLent: Double,
    val thisMonthReturned: Double,
    val thisMonthNewLoansCount: Int,
    val thisMonthRepaymentsCount: Int,
    val lifetimeLent: Double,
    val lifetimeReturned: Double
)

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val date: Long,
    val isPayment: Boolean,
    val direction: LoanDirection,
    val loanId: Long,
    val personName: String,
    val paymentMethod: String
)

data class ReminderItem(
    val id: Long,
    val loanId: Long,
    val personName: String,
    val personPhone: String,
    val loanPurpose: String,
    val outstandingAmount: Double,
    val dueDate: Long?,
    val status: LoanStatus,
    val message: String,
    val reminderType: String,
    val reminderDate: Long,
    val isCompleted: Boolean
)

object Formatters {
    private val currencyFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatMoney(amount: Double, currencySymbol: String = "৳"): String {
        return "$currencySymbol${currencyFormat.format(amount)}"
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "No fixed date"
        return dateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return "${dateFormat.format(Date(timestamp))} at ${timeFormat.format(Date(timestamp))}"
    }
}
