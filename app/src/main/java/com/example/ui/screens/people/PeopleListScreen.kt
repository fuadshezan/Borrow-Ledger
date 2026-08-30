package com.example.ui.screens.people

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Formatters
import com.example.data.model.PersonSummary
import com.example.ui.components.EmptyState
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel

enum class PeopleFilter(val label: String) {
    ALL("All Contacts"),
    OWES_ME("Owes Me"),
    I_OWE("I Owe"),
    SETTLED("Settled")
}

@Composable
fun PeopleListScreen(
    viewModel: LendingViewModel,
    onNavigateToPersonDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val people by viewModel.allPeople.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PeopleFilter.ALL) }
    var showAddPersonDialog by remember { mutableStateOf(false) }

    val filteredPeople = people.filter { person ->
        val queryMatch = searchQuery.isBlank() ||
                person.name.contains(searchQuery, ignoreCase = true) ||
                person.phone.contains(searchQuery, ignoreCase = true)

        val filterMatch = when (selectedFilter) {
            PeopleFilter.ALL -> true
            PeopleFilter.OWES_ME -> person.totalLentOutstanding > 0.001
            PeopleFilter.I_OWE -> person.totalBorrowedOutstanding > 0.001
            PeopleFilter.SETTLED -> person.totalLentOutstanding <= 0.001 && person.totalBorrowedOutstanding <= 0.001 && (person.totalLent > 0 || person.totalBorrowed > 0)
        }

        queryMatch && filterMatch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPersonDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("people_list_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Person")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("people_list_screen")
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("people_search_input"),
                placeholder = { Text("Search by name or phone...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PeopleFilter.values()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("people_filter_chip_${filter.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPeople.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.People,
                    title = "No People Found",
                    subtitle = if (searchQuery.isNotBlank()) "No contact matching '$searchQuery'" else "Add your contacts to record loans and repayments.",
                    actionLabel = "+ Add Person",
                    onAction = { showAddPersonDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredPeople, key = { it.id }) { person ->
                        PersonCardItem(
                            person = person,
                            currencySymbol = currency,
                            onClick = { onNavigateToPersonDetail(person.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onConfirm = { name, phone, email, notes ->
                viewModel.addPerson(name, phone, email, notes)
                showAddPersonDialog = false
            }
        )
    }
}

@Composable
fun PersonCardItem(
    person: PersonSummary,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("person_card_${person.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = person.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (person.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = person.phone,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${person.activeLoansCount} active • ${person.settledLoansCount} settled",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (person.totalLentOutstanding > 0.001) {
                    Text(
                        text = Formatters.formatMoney(person.totalLentOutstanding, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppTheme.colors.greenText
                    )
                    Text(
                        text = "Owes you",
                        fontSize = 11.sp,
                        color = AppTheme.colors.greenText,
                        fontWeight = FontWeight.Medium
                    )
                } else if (person.totalBorrowedOutstanding > 0.001) {
                    Text(
                        text = Formatters.formatMoney(person.totalBorrowedOutstanding, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppTheme.colors.purpleText
                    )
                    Text(
                        text = "You owe",
                        fontSize = 11.sp,
                        color = AppTheme.colors.purpleText,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Surface(
                        color = AppTheme.colors.greenContainer,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.greenBorder)
                    ) {
                        Text(
                            text = "Settled ✓",
                            color = AppTheme.colors.greenText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (person.hasOverdue) {
                    Surface(
                        color = AppTheme.colors.redContainer,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.redBorder),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Overdue",
                            color = AppTheme.colors.redText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Person", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Full Name *") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_person_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_person_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Relationship / Context") },
                    placeholder = { Text("e.g. University friend, Cousin") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name, phone, email, notes)
                    }
                },
                modifier = Modifier.testTag("add_person_save_button")
            ) {
                Text("Save Person")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
