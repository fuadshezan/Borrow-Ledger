package com.example.ui.screens.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Formatters
import com.example.data.model.LoanDirection
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentItem
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DirectionBadge
import com.example.ui.components.LoanProgressBar
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.FinanceAmberDark
import com.example.ui.theme.FinanceAmberLight
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceGreenDark
import com.example.ui.theme.FinanceGreenLight
import com.example.ui.theme.FinanceGreenBorder
import com.example.ui.theme.FinanceRed
import com.example.ui.theme.FinanceRedDark
import com.example.ui.theme.FinanceRedLight
import com.example.ui.theme.FinanceRedBorder
import com.example.ui.theme.FinanceAmberBorder
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.LendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    viewModel: LendingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPersonDetail: (Long) -> Unit,
    onNavigateToAddPayment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allLoans by viewModel.allLoans.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val loan = allLoans.find { it.id == loanId }

    var showDeleteLoanDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<PaymentItem?>(null) }

    if (loan == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loan not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("loan_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteLoanDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Loan",
                            tint = FinanceRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("loan_detail_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Person Link Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPersonDetail(loan.personId) }
                        .testTag("loan_detail_person_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Indigo50),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = loan.personName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Indigo600
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loan.personName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (loan.personPhone.isNotBlank()) {
                                Text(
                                    text = loan.personPhone,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DirectionBadge(direction = loan.direction)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // 2. Primary Financial Balance Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (loan.isSettled) FinanceGreenLight else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (loan.isSettled) FinanceGreenBorder else Slate200
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (loan.direction == LoanDirection.LENT) "Remaining to Receive" else "Remaining to Pay",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (loan.isSettled) FinanceGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusBadge(status = loan.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = Formatters.formatMoney(loan.outstanding, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (loan.isSettled) FinanceGreenDark else if (loan.status == LoanStatus.OVERDUE) FinanceRedDark else Indigo600
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Original Loan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatMoney(loan.originalAmount, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Returned", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatMoney(loan.totalPaid, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = FinanceGreenDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        LoanProgressBar(totalPaid = loan.totalPaid, originalAmount = loan.originalAmount)
                    }
                }
            }

            // 3. Due Date & Status Banner
            item {
                if (loan.status == LoanStatus.OVERDUE) {
                    Surface(
                        color = FinanceRedLight,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FinanceRedBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = FinanceRedDark)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Overdue by ${loan.daysOverdue} days!",
                                    fontWeight = FontWeight.Bold,
                                    color = FinanceRedDark,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Was due on ${Formatters.formatDate(loan.dueDate)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else if (loan.status == LoanStatus.DUE_SOON) {
                    Surface(
                        color = FinanceAmberLight,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FinanceAmberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = FinanceAmberDark)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Due soon in ${loan.daysUntilDue ?: 0} days",
                                    fontWeight = FontWeight.Bold,
                                    color = FinanceAmberDark,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Due date: ${Formatters.formatDate(loan.dueDate)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4. Primary Action Button: Record Repayment
            if (!loan.isSettled) {
                item {
                    Button(
                        onClick = { onNavigateToAddPayment(loan.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("loan_detail_record_payment_button")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record Repayment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 5. Loan Metadata Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Loan Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        MetadataRow(label = "Purpose / Reason", value = loan.purpose.ifBlank { "Not specified" })
                        MetadataRow(label = "Date Given", value = Formatters.formatDate(loan.loanDate))
                        MetadataRow(label = "Due Date", value = Formatters.formatDate(loan.dueDate))
                        MetadataRow(label = "Payment Method", value = loan.paymentMethod)

                        if (loan.installmentAmount != null && loan.installmentAmount > 0) {
                            MetadataRow(
                                label = "Installment Plan",
                                value = "${Formatters.formatMoney(loan.installmentAmount, currency)} / ${loan.installmentFrequency ?: "Monthly"} (${loan.installmentCount ?: "-"} installments)"
                            )
                        }

                        if (loan.note.isNotBlank()) {
                            MetadataRow(label = "Notes", value = loan.note)
                        }
                    }
                }
            }

            // 6. Payment History List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Repayment History (${loan.payments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (loan.payments.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No repayments recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(loan.payments, key = { it.id }) { payment ->
                    PaymentHistoryItem(
                        payment = payment,
                        currencySymbol = currency,
                        onDelete = { paymentToDelete = payment }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Delete Loan confirmation
    if (showDeleteLoanDialog) {
        ConfirmDeleteDialog(
            title = "Delete this loan?",
            message = "This will permanently delete this loan of ${Formatters.formatMoney(loan.originalAmount, currency)} and all ${loan.payments.size} repayment entries.",
            impactNote = "This cannot be undone.",
            onConfirm = {
                viewModel.deleteLoan(loan.id, loan.purpose.ifBlank { "Loan #${loan.id}" })
                onNavigateBack()
            },
            onDismiss = { showDeleteLoanDialog = false }
        )
    }

    // Delete Payment confirmation
    if (paymentToDelete != null) {
        val p = paymentToDelete!!
        ConfirmDeleteDialog(
            title = "Delete this payment?",
            message = "Are you sure you want to delete this payment of ${Formatters.formatMoney(p.amount, currency)} recorded on ${Formatters.formatDate(p.paymentDate)}?",
            impactNote = "Notice: Deleting this payment will restore ৳${p.amount.toInt()} back to the outstanding balance.",
            onConfirm = {
                viewModel.deletePayment(p.id, p.amount)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun PaymentHistoryItem(
    payment: PaymentItem,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_history_item_${payment.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(FinanceGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FinanceGreenDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "+ ${Formatters.formatMoney(payment.amount, currencySymbol)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FinanceGreenDark
                )
                Text(
                    text = "${Formatters.formatDate(payment.paymentDate)} • ${payment.paymentMethod}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.note.isNotBlank()) {
                    Text(
                        text = "\"${payment.note}\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_payment_btn_${payment.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Payment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
