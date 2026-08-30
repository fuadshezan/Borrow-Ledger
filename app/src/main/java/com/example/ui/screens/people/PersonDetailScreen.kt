package com.example.ui.screens.people

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Formatters
import com.example.data.model.LoanDirection
import com.example.data.model.LoanWithDetails
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DirectionBadge
import com.example.ui.components.LoanProgressBar
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    viewModel: LendingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit,
    onNavigateToAddLoanForPerson: (Long) -> Unit,
    onNavigateToAddPaymentForLoan: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val people by viewModel.allPeople.collectAsStateWithLifecycle()
    val allLoans by viewModel.allLoans.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val person = people.find { it.id == personId }
    val personLoans = allLoans.filter { it.personId == personId }
    val activeLoans = personLoans.filter { !it.isSettled }
    val settledLoans = personLoans.filter { it.isSettled }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (person == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Person not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("person_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Person",
                            tint = AppTheme.colors.redText
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
                .testTag("person_detail_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Person Header Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.iconBoxBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = AppTheme.colors.iconBoxTint
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (person.notes.isNotBlank()) {
                                    Text(
                                        text = person.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (person.phone.isNotBlank()) {
                                    Text(
                                        text = person.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Contact shortcuts
                        if (person.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${person.phone}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SMS")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Hi ${person.name}, friendly reminder regarding your outstanding balance of ${Formatters.formatMoney(person.totalLentOutstanding, currency)}."
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Reminder"))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remind")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Financial Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Financial Summary",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Lent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatMoney(person.totalLent, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Returned", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatMoney(person.totalLentReturned, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AppTheme.colors.greenText
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Outstanding", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatMoney(person.totalLentOutstanding, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (person.totalLentOutstanding > 0) AppTheme.colors.redText else AppTheme.colors.greenText
                                )
                            }
                        }
                    }
                }
            }

            // 3. Action Row: Add New Loan for this person
            item {
                Button(
                    onClick = { onNavigateToAddLoanForPerson(person.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("person_detail_add_loan_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Loan for ${person.name}", fontWeight = FontWeight.Bold)
                }
            }

            // 4. Tabs: Active Loans vs Settled Loans vs Timeline
            item {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Active (${activeLoans.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Settled (${settledLoans.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Timeline", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Tab 0: Active Loans
            if (selectedTabIndex == 0) {
                if (activeLoans.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No active loans for ${person.name}. All settled! 🎉",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(activeLoans, key = { it.id }) { loan ->
                        PersonLoanItemCard(
                            loan = loan,
                            currencySymbol = currency,
                            onOpenDetails = { onNavigateToLoanDetail(loan.id) },
                            onRecordPayment = { onNavigateToAddPaymentForLoan(loan.id) }
                        )
                    }
                }
            }

            // Tab 1: Settled Loans
            if (selectedTabIndex == 1) {
                if (settledLoans.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No settled loans yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(settledLoans, key = { it.id }) { loan ->
                        PersonLoanItemCard(
                            loan = loan,
                            currencySymbol = currency,
                            onOpenDetails = { onNavigateToLoanDetail(loan.id) },
                            onRecordPayment = null
                        )
                    }
                }
            }

            // Tab 2: Chronological Timeline
            if (selectedTabIndex == 2) {
                val timelineList = mutableListOf<PersonTimelineEvent>()
                personLoans.forEach { loan ->
                    timelineList.add(
                        PersonTimelineEvent(
                            id = "loan_${loan.id}",
                            title = if (loan.direction == LoanDirection.LENT) "Lent ${loan.purpose.ifBlank { "Money" }}" else "Borrowed ${loan.purpose.ifBlank { "Money" }}",
                            amount = loan.originalAmount,
                            date = loan.loanDate,
                            isPayment = false,
                            method = loan.paymentMethod,
                            note = loan.note
                        )
                    )
                    loan.payments.forEach { payment ->
                        timelineList.add(
                            PersonTimelineEvent(
                                id = "pay_${payment.id}",
                                title = "Payment Received",
                                amount = payment.amount,
                                date = payment.paymentDate,
                                isPayment = true,
                                method = payment.paymentMethod,
                                note = payment.note
                            )
                        )
                    }
                }
                val sortedTimeline = timelineList.sortedByDescending { it.date }

                if (sortedTimeline.isEmpty()) {
                    item {
                        Text(
                            text = "No transactions recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(sortedTimeline, key = { it.id }) { event ->
                        PersonTimelineRow(event = event, currencySymbol = currency)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete ${person.name}?",
            message = "This will permanently remove ${person.name} and all ${personLoans.size} associated loans and payment records.",
            impactNote = "Warning: Outstanding balance of ${Formatters.formatMoney(person.totalLentOutstanding, currency)} will be erased from records.",
            onConfirm = {
                viewModel.deletePerson(person.id, person.name)
                onNavigateBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

private data class PersonTimelineEvent(
    val id: String,
    val title: String,
    val amount: Double,
    val date: Long,
    val isPayment: Boolean,
    val method: String,
    val note: String
)

@Composable
private fun PersonLoanItemCard(
    loan: LoanWithDetails,
    currencySymbol: String,
    onOpenDetails: () -> Unit,
    onRecordPayment: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
            .testTag("person_loan_card_${loan.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DirectionBadge(direction = loan.direction)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (loan.purpose.isNotBlank()) loan.purpose else "Loan #${loan.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StatusBadge(status = loan.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Original", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(loan.originalAmount, currencySymbol),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Returned", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(loan.totalPaid, currencySymbol),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AppTheme.colors.greenText
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(loan.outstanding, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (loan.isSettled) AppTheme.colors.greenText else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LoanProgressBar(totalPaid = loan.totalPaid, originalAmount = loan.originalAmount)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (loan.dueDate != null) "Due: ${Formatters.formatDate(loan.dueDate)}" else "No fixed due date",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (onRecordPayment != null && !loan.isSettled) {
                    FilledTonalButton(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("loan_quick_pay_button_${loan.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonTimelineRow(
    event: PersonTimelineEvent,
    currencySymbol: String
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (event.isPayment) AppTheme.colors.greenContainer else AppTheme.colors.purpleContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (event.isPayment) Icons.Default.Payment else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (event.isPayment) AppTheme.colors.greenText else AppTheme.colors.purpleText,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${Formatters.formatDate(event.date)} • ${event.method}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.note.isNotBlank()) {
                    Text(
                        text = "\"${event.note}\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = (if (event.isPayment) "-" else "+") + Formatters.formatMoney(event.amount, currencySymbol),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (event.isPayment) AppTheme.colors.greenText else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
