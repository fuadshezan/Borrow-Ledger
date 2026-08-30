package com.example.ui.screens.payments

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Formatters
import com.example.data.model.LoanDirection
import com.example.data.model.LoanWithDetails
import com.example.data.model.PaymentMethodOption
import com.example.ui.components.DirectionBadge
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentScreen(
    initialLoanId: Long? = null,
    viewModel: LendingViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLoans by viewModel.allLoans.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val activeLoans = allLoans.filter { !it.isSettled }
    var selectedLoanId by remember {
        mutableStateOf(initialLoanId ?: activeLoans.firstOrNull()?.id)
    }
    var loanDropdownExpanded by remember { mutableStateOf(false) }

    val selectedLoan: LoanWithDetails? = allLoans.find { it.id == selectedLoanId }

    var amountText by remember { mutableStateOf("") }
    var paymentDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var paymentMethod by remember { mutableStateOf("bKash") }
    var note by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val c = Calendar.getInstance().apply { set(y, m, d) }
            paymentDate = c.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val currentOutstanding = selectedLoan?.outstanding ?: 0.0
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isOverpaid = parsedAmount > (currentOutstanding + 0.001)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Repayment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_payment_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .testTag("add_payment_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Select Active Loan
            item {
                Column {
                    Text(
                        text = "Select Loan to Repay *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (activeLoans.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "There are no active loans to repay. All loans are settled!",
                                modifier = Modifier.padding(14.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = loanDropdownExpanded,
                            onExpandedChange = { loanDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedLoan?.let {
                                    "${it.personName} - ${if (it.purpose.isNotBlank()) it.purpose else "Loan"} (Remaining: ${Formatters.formatMoney(it.outstanding, currency)})"
                                } ?: "Select a loan",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = loanDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("add_payment_loan_selector")
                            )

                            ExposedDropdownMenu(
                                expanded = loanDropdownExpanded,
                                onDismissRequest = { loanDropdownExpanded = false }
                            ) {
                                activeLoans.forEach { loan ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(loan.personName, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    DirectionBadge(direction = loan.direction)
                                                }
                                                Text(
                                                    text = "${loan.purpose.ifBlank { "Loan" }} • Remaining: ${Formatters.formatMoney(loan.outstanding, currency)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedLoanId = loan.id
                                            loanDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Outstanding Balance Card
            if (selectedLoan != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.greenContainer),
                        border = BorderStroke(1.dp, AppTheme.colors.greenBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (selectedLoan.direction == LoanDirection.LENT) "Remaining balance owed to you" else "Remaining balance you owe",
                                fontSize = 12.sp,
                                color = AppTheme.colors.greenText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatMoney(currentOutstanding, currency),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.greenText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Original Loan: ${Formatters.formatMoney(selectedLoan.originalAmount, currency)} • Already Paid: ${Formatters.formatMoney(selectedLoan.totalPaid, currency)}",
                                fontSize = 12.sp,
                                color = AppTheme.colors.greenText.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 3. Amount Field + Quick Amount Presets
            if (selectedLoan != null) {
                item {
                    Column {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Repayment Amount ($currency) *") },
                            placeholder = { Text("e.g. 5000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isOverpaid,
                            supportingText = if (isOverpaid) {
                                { Text("Cannot exceed outstanding balance of ${Formatters.formatMoney(currentOutstanding, currency)}") }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_payment_amount_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick fill buttons
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = amountText == currentOutstanding.toInt().toString(),
                                    onClick = { amountText = currentOutstanding.toInt().toString() },
                                    label = { Text("Full (${Formatters.formatMoney(currentOutstanding, currency)})", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("add_payment_quick_full_chip")
                                )
                            }
                            if (currentOutstanding > 1000) {
                                item {
                                    val half = (currentOutstanding / 2).toInt()
                                    FilterChip(
                                        selected = amountText == half.toString(),
                                        onClick = { amountText = half.toString() },
                                        label = { Text("Half (${Formatters.formatMoney(half.toDouble(), currency)})", fontSize = 11.sp) }
                                    )
                                }
                            }
                            if (selectedLoan.installmentAmount != null && selectedLoan.installmentAmount > 0) {
                                item {
                                    val inst = selectedLoan.installmentAmount.toInt()
                                    FilterChip(
                                        selected = amountText == inst.toString(),
                                        onClick = { amountText = inst.toString() },
                                        label = { Text("1 Installment (${Formatters.formatMoney(inst.toDouble(), currency)})", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Overpayment warning banner
                if (isOverpaid) {
                    item {
                        Surface(
                            color = AppTheme.colors.redContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppTheme.colors.redBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AppTheme.colors.redText)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Overpayment is not allowed. The maximum amount you can record is ${Formatters.formatMoney(currentOutstanding, currency)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.redText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 5. Payment Date
                item {
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePicker.show() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Payment Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Formatters.formatDate(paymentDate),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    }
                }

                // 6. Payment Method Selection
                item {
                    Column {
                        Text(
                            text = "Payment Method Received Via",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PaymentMethodOption.values()) { opt ->
                                FilterChip(
                                    selected = paymentMethod == opt.displayName,
                                    onClick = { paymentMethod = opt.displayName },
                                    label = { Text(opt.displayName, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                // 7. Note
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note / Memo") },
                        placeholder = { Text("e.g. 2nd installment, paid via bKash TRX: 8X9Y7Z") },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_payment_note_input")
                    )
                }

                // 8. Submit Button
                item {
                    Button(
                        onClick = {
                            val loanId = selectedLoanId ?: return@Button
                            if (parsedAmount <= 0 || isOverpaid) return@Button

                            viewModel.recordPayment(
                                loanId = loanId,
                                amount = parsedAmount,
                                paymentDate = paymentDate,
                                paymentMethod = paymentMethod,
                                note = note,
                                onSuccess = {
                                    onPaymentSaved()
                                }
                            )
                        },
                        enabled = parsedAmount > 0 && !isOverpaid && selectedLoanId != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_payment_submit_button")
                    ) {
                        Text("Record Repayment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
