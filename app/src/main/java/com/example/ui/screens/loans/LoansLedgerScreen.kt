package com.example.ui.screens.loans

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Formatters
import com.example.data.model.LoanDirection
import com.example.data.model.LoanWithDetails
import com.example.ui.components.DirectionBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.LoanProgressBar
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel
import com.example.ui.viewmodel.LoanFilterOption

@Composable
fun LoansLedgerScreen(
    viewModel: LendingViewModel,
    onNavigateToLoanDetail: (Long) -> Unit,
    onNavigateToAddLoan: () -> Unit,
    onNavigateToAddPayment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLoans by viewModel.filteredLoans.collectAsStateWithLifecycle()
    val statusFilter by viewModel.loanStatusFilter.collectAsStateWithLifecycle()
    val directionFilter by viewModel.directionFilter.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val displayedLoans = filteredLoans.filter { loan ->
        searchQuery.isBlank() ||
                loan.personName.contains(searchQuery, ignoreCase = true) ||
                loan.purpose.contains(searchQuery, ignoreCase = true) ||
                loan.note.contains(searchQuery, ignoreCase = true) ||
                loan.paymentMethod.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddLoan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("loans_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Loan")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("loans_ledger_screen")
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("loans_search_input"),
                placeholder = { Text("Search by person, purpose, note...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LoanFilterOption.values()) { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { viewModel.setLoanStatusFilter(filter) },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("loan_filter_chip_${filter.name}")
                    )
                }
            }

            // Direction Sub-filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = directionFilter == null,
                    onClick = { viewModel.setDirectionFilter(null) },
                    label = { Text("All Types", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = directionFilter == LoanDirection.LENT,
                    onClick = { viewModel.setDirectionFilter(LoanDirection.LENT) },
                    label = { Text("Lent Only", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = directionFilter == LoanDirection.BORROWED,
                    onClick = { viewModel.setDirectionFilter(LoanDirection.BORROWED) },
                    label = { Text("Borrowed Only", fontSize = 11.sp) }
                )
            }

            if (displayedLoans.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ReceiptLong,
                    title = "No Loans Found",
                    subtitle = if (searchQuery.isNotBlank()) "No records match '$searchQuery'" else "You have no loans under the selected filter.",
                    actionLabel = "+ Create Loan",
                    onAction = onNavigateToAddLoan,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayedLoans, key = { it.id }) { loan ->
                        LoanLedgerCard(
                            loan = loan,
                            currencySymbol = currency,
                            onClick = { onNavigateToLoanDetail(loan.id) },
                            onQuickPay = { onNavigateToAddPayment(loan.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanLedgerCard(
    loan: LoanWithDetails,
    currencySymbol: String,
    onClick: () -> Unit,
    onQuickPay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("loan_ledger_card_${loan.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Direction, Name, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    DirectionBadge(direction = loan.direction)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = loan.personName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(status = loan.status)
            }

            if (loan.purpose.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = loan.purpose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Original", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(loan.originalAmount, currencySymbol),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Returned", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(loan.totalPaid, currencySymbol),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AppTheme.colors.greenText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(loan.outstanding, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (loan.isSettled) AppTheme.colors.greenText else if (loan.status == com.example.data.model.LoanStatus.OVERDUE) AppTheme.colors.redText else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LoanProgressBar(totalPaid = loan.totalPaid, originalAmount = loan.originalAmount)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (loan.dueDate != null) "Due: ${Formatters.formatDate(loan.dueDate)}" else "Given: ${Formatters.formatDate(loan.loanDate)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PaymentMethodBadge(method = loan.paymentMethod)
                }

                if (!loan.isSettled) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onQuickPay() }
                            .testTag("loan_card_record_payment_btn_${loan.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Record Pay",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
