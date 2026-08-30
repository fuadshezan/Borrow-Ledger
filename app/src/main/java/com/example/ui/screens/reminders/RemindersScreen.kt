package com.example.ui.screens.reminders

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Formatters
import com.example.data.model.LoanStatus
import com.example.data.model.ReminderItem
import com.example.ui.components.EmptyState
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: LendingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit,
    onNavigateToAddPayment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reminders by viewModel.reminderItems.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    val pending = reminders.filter { !it.isCompleted }
    val completed = reminders.filter { it.isCompleted }

    val displayedList = if (selectedTab == 0) pending else completed

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repayment Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reminders_back_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("reminders_screen")
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Reminders (${pending.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Completed (${completed.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (displayedList.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.NotificationsActive,
                    title = if (selectedTab == 0) "No Pending Reminders" else "No Completed Reminders",
                    subtitle = if (selectedTab == 0) "All your loans are on track with no pending due dates." else "Completed reminders will appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedList, key = { it.id }) { item ->
                        ReminderCard(
                            item = item,
                            currencySymbol = currency,
                            onToggleCompleted = { isChecked ->
                                viewModel.toggleReminder(item.id, isChecked)
                            },
                            onClick = { onNavigateToLoanDetail(item.loanId) },
                            onQuickPay = { onNavigateToAddPayment(item.loanId) },
                            onSendReminder = {
                                val text = "Hi ${item.personName}, friendly reminder about the repayment of ${Formatters.formatMoney(item.outstandingAmount, currency)} due on ${Formatters.formatDate(item.dueDate)}."
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Send Reminder"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    item: ReminderItem,
    currencySymbol: String,
    onToggleCompleted: (Boolean) -> Unit,
    onClick: () -> Unit,
    onQuickPay: () -> Unit,
    onSendReminder: () -> Unit
) {
    val isOverdue = item.status == LoanStatus.OVERDUE

    val borderColor = if (item.isCompleted) AppTheme.colors.cardBorder
    else if (isOverdue) AppTheme.colors.redBorder
    else AppTheme.colors.amberBorder

    val cardBg = if (item.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    else if (isOverdue) AppTheme.colors.redContainer
    else AppTheme.colors.amberContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("reminder_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = onToggleCompleted
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.personName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = item.message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatMoney(item.outstandingAmount, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isOverdue) AppTheme.colors.redText else MaterialTheme.colorScheme.primary
                    )
                    StatusBadge(status = item.status)
                }
            }

            if (!item.isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSendReminder,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Reminder", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = onQuickPay,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Pay", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
