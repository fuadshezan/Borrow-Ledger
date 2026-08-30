package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActivityItem
import com.example.data.model.Formatters
import com.example.data.model.LoanDirection
import com.example.data.model.PersonSummary
import com.example.ui.components.DirectionBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.PaymentMethodBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.SummaryCard
import com.example.ui.theme.FinanceAmber
import com.example.ui.theme.FinanceAmberBorder
import com.example.ui.theme.FinanceAmberDark
import com.example.ui.theme.FinanceAmberLight
import com.example.ui.theme.FinanceBlue
import com.example.ui.theme.FinanceBlueLight
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceGreenBorder
import com.example.ui.theme.FinanceGreenDark
import com.example.ui.theme.FinanceGreenLight
import com.example.ui.theme.FinancePurple
import com.example.ui.theme.FinancePurpleDark
import com.example.ui.theme.FinancePurpleLight
import com.example.ui.theme.FinanceRed
import com.example.ui.theme.FinanceRedBorder
import com.example.ui.theme.FinanceRedDark
import com.example.ui.theme.FinanceRedLight
import com.example.ui.theme.Indigo200
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.viewmodel.LendingViewModel

@Composable
fun DashboardScreen(
    viewModel: LendingViewModel,
    onNavigateToLoanDetail: (Long) -> Unit,
    onNavigateToPersonDetail: (Long) -> Unit,
    onNavigateToAddLoan: () -> Unit,
    onNavigateToAddPayment: (Long?) -> Unit,
    onNavigateToAddPerson: () -> Unit,
    onNavigateToWhoOwesMe: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onOpenQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val whoOwesMe by viewModel.whoOwesMeList.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivity.collectAsStateWithLifecycle()
    val reminders by viewModel.reminderItems.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val pendingReminders = reminders.filter { !it.isCompleted }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Greeting & Date
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Financial Overview 👋",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = Formatters.formatDate(System.currentTimeMillis()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Add Highlight button in header
                    Button(
                        onClick = onOpenQuickAdd,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        modifier = Modifier.testTag("dashboard_quick_add_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quick Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Summary Cards Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "People Owe Me",
                        amount = summary.peopleOweMeTotal,
                        currencySymbol = currency,
                        subtitle = "${summary.activeDebtorsCount} people owe you",
                        icon = Icons.Default.CallReceived,
                        cardColor = FinanceGreenLight,
                        accentColor = FinanceGreenDark,
                        borderColor = FinanceGreenBorder,
                        onClick = onNavigateToWhoOwesMe,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_people_owe_me")
                    )
                    SummaryCard(
                        title = "I Owe Others",
                        amount = summary.iOweOthersTotal,
                        currencySymbol = currency,
                        subtitle = "Outstanding borrowed",
                        icon = Icons.Default.CallMade,
                        cardColor = FinancePurpleLight,
                        accentColor = FinancePurpleDark,
                        borderColor = Indigo200,
                        onClick = onNavigateToLoans,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_i_owe_others")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Overdue",
                        amount = summary.overdueTotal,
                        currencySymbol = currency,
                        subtitle = "Needs urgent follow-up",
                        icon = Icons.Default.ErrorOutline,
                        cardColor = FinanceRedLight,
                        accentColor = FinanceRedDark,
                        borderColor = FinanceRedBorder,
                        onClick = onNavigateToReminders,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_overdue")
                    )
                    SummaryCard(
                        title = "Due Soon",
                        amount = summary.dueSoonTotal,
                        currencySymbol = currency,
                        subtitle = "Within next 7 days",
                        icon = Icons.Default.HourglassEmpty,
                        cardColor = FinanceAmberLight,
                        accentColor = FinanceAmberDark,
                        borderColor = FinanceAmberBorder,
                        onClick = onNavigateToReminders,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_due_soon")
                    )
                }
            }
        }

        // 3. Quick Action Shortcut Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToAddLoan,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_add_loan_action_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Loan", fontSize = 12.sp, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = { onNavigateToAddPayment(null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_add_payment_action_button")
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Payment", fontSize = 12.sp, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onNavigateToAddPerson,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_add_person_action_button")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Person", fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        // 4. Overdue / Due Soon Alert Banner (if any)
        if (pendingReminders.isNotEmpty()) {
            item {
                val topReminder = pendingReminders.first()
                val isOverdue = topReminder.status == com.example.data.model.LoanStatus.OVERDUE

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOverdue) FinanceRedLight else FinanceAmberLight
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOverdue) FinanceRedBorder else FinanceAmberBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReminders() }
                        .testTag("dashboard_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isOverdue) FinanceRed.copy(alpha = 0.15f) else FinanceAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isOverdue) FinanceRedDark else FinanceAmberDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isOverdue) "Overdue Alert" else "Upcoming Repayment Due",
                                fontWeight = FontWeight.Bold,
                                color = if (isOverdue) FinanceRedDark else FinanceAmberDark,
                                fontSize = 13.sp
                            )
                            Text(
                                text = topReminder.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View Reminders",
                            tint = if (isOverdue) FinanceRedDark else FinanceAmberDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 5. "Who Owes Me" Horizontal Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Who Owes Me",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onNavigateToWhoOwesMe,
                    modifier = Modifier.testTag("dashboard_see_all_debtors_button")
                ) {
                    Text("See All (${whoOwesMe.size})", fontWeight = FontWeight.SemiBold)
                }
            }

            if (whoOwesMe.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No one currently owes you money! 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(whoOwesMe.take(5), key = { it.id }) { person ->
                        WhoOwesMeMiniCard(
                            person = person,
                            currencySymbol = currency,
                            onClick = { onNavigateToPersonDetail(person.id) }
                        )
                    }
                }
            }
        }

        // 6. Recent Activity Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onNavigateToLoans,
                    modifier = Modifier.testTag("dashboard_see_all_activity_button")
                ) {
                    Text("Ledger", fontWeight = FontWeight.SemiBold)
                }
            }

            if (recentActivities.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.TrendingUp,
                    title = "No Recent Activity",
                    subtitle = "Start by adding a loan or recording repayments.",
                    actionLabel = "+ Add Loan",
                    onAction = onNavigateToAddLoan
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentActivities.take(6).forEach { activity ->
                        ActivityRowItem(
                            activity = activity,
                            currencySymbol = currency,
                            onClick = { onNavigateToLoanDetail(activity.loanId) }
                        )
                    }
                }
            }
        }

        // 7. Monthly & Lifetime Insights Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_insights_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Monthly & Lifetime Insights",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("This Month Lent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatMoney(summary.thisMonthLent, currency),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FinanceGreenDark
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("This Month Returned", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatMoney(summary.thisMonthReturned, currency),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lifetime Lent: ${Formatters.formatMoney(summary.lifetimeLent, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Settled: ${summary.settledLoansCount} loans",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinanceGreenDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun WhoOwesMeMiniCard(
    person: PersonSummary,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() }
            .testTag("who_owes_me_mini_card_${person.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = person.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (person.hasOverdue) {
                    Surface(
                        color = FinanceRedLight,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FinanceRedBorder)
                    ) {
                        Text(
                            text = "Overdue",
                            color = FinanceRedDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (person.hasDueSoon) {
                    Surface(
                        color = FinanceAmberLight,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FinanceAmberBorder)
                    ) {
                        Text(
                            text = "Due Soon",
                            color = FinanceAmberDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = person.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Formatters.formatMoney(person.totalLentOutstanding, currencySymbol),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = FinanceGreenDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${person.activeLoansCount} active loan${if (person.activeLoansCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ActivityRowItem(
    activity: ActivityItem,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("activity_item_${activity.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, bg, tint) = if (activity.isPayment) {
                Triple(Icons.Default.CheckCircle, FinanceGreenLight, FinanceGreenDark)
            } else {
                Triple(Icons.Default.CallMade, FinancePurpleLight, FinancePurpleDark)
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatDate(activity.date),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activity.paymentMethod.isNotBlank()) {
                        Text(
                            text = " • ${activity.paymentMethod}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (activity.isPayment) "+" else "") + Formatters.formatMoney(activity.amount, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (activity.isPayment) FinanceGreenDark else MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (activity.isPayment) FinanceGreenLight else FinancePurpleLight,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = if (activity.isPayment) "Payment" else "Loan",
                        color = if (activity.isPayment) FinanceGreenDark else FinancePurpleDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
