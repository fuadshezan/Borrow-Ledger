package com.example.ui.screens.whoowes

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Formatters
import com.example.data.model.PersonSummary
import com.example.ui.components.EmptyState
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoOwesMeScreen(
    viewModel: LendingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPersonDetail: (Long) -> Unit,
    onNavigateToAddLoan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val whoOwesMeList by viewModel.whoOwesMeList.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val totalOwed = whoOwesMeList.sumOf { it.totalLentOutstanding }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Who Owes Me", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("who_owes_me_back_button")
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
                .testTag("who_owes_me_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Total Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.greenContainer),
                    border = BorderStroke(1.dp, AppTheme.colors.greenBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Total Money Owed to You",
                            fontSize = 13.sp,
                            color = AppTheme.colors.greenText,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(totalOwed, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppTheme.colors.greenText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Across ${whoOwesMeList.size} borrowers",
                            fontSize = 12.sp,
                            color = AppTheme.colors.greenText.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (whoOwesMeList.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.CheckCircle,
                        title = "No Outstanding Debts",
                        subtitle = "Everyone has settled their loans or no loans recorded yet.",
                        actionLabel = "+ Create Loan",
                        onAction = onNavigateToAddLoan
                    )
                }
            } else {
                items(whoOwesMeList, key = { it.id }) { person ->
                    DebtorCardItem(
                        person = person,
                        currencySymbol = currency,
                        onClick = { onNavigateToPersonDetail(person.id) },
                        onSendReminder = {
                            val text = "Hi ${person.name}, gentle reminder regarding the outstanding balance of ${Formatters.formatMoney(person.totalLentOutstanding, currency)}."
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send Reminder"))
                        },
                        onCall = {
                            if (person.phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DebtorCardItem(
    person: PersonSummary,
    currencySymbol: String,
    onClick: () -> Unit,
    onSendReminder: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("debtor_card_${person.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = person.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = person.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (person.phone.isNotBlank()) {
                        Text(
                            text = person.phone,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatMoney(person.totalLentOutstanding, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (person.hasOverdue) AppTheme.colors.redText else AppTheme.colors.greenText
                    )
                    if (person.hasOverdue) {
                        Surface(
                            color = AppTheme.colors.redContainer,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, AppTheme.colors.redBorder),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Overdue",
                                color = AppTheme.colors.redText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else if (person.hasDueSoon) {
                        Surface(
                            color = AppTheme.colors.amberContainer,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, AppTheme.colors.amberBorder),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Due Soon",
                                color = AppTheme.colors.amberText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Lent: ${Formatters.formatMoney(person.totalLent, currencySymbol)} • Paid: ${Formatters.formatMoney(person.totalLentReturned, currencySymbol)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${person.activeLoansCount} active loan(s)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Send Reminder & Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSendReminder,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remind", fontSize = 12.sp)
                }

                if (person.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
