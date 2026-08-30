package com.example.ui.screens.loans

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.model.LoanPurposeCategory
import com.example.data.model.PaymentMethodOption
import com.example.ui.screens.people.AddPersonDialog
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    initialPersonId: Long? = null,
    viewModel: LendingViewModel,
    onNavigateBack: () -> Unit,
    onLoanCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val people by viewModel.allPeople.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var selectedPersonId by remember { mutableStateOf(initialPersonId ?: people.firstOrNull()?.id) }
    var personDropdownExpanded by remember { mutableStateOf(false) }
    var showInlineAddPersonDialog by remember { mutableStateOf(false) }

    var direction by remember { mutableStateOf(LoanDirection.LENT) }
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    var loanDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hasDueDate by remember { mutableStateOf(true) }
    var dueDate by remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 14) // default 2 weeks
        mutableStateOf<Long?>(cal.timeInMillis)
    }

    var purpose by remember { mutableStateOf("Emergency") }
    var paymentMethod by remember { mutableStateOf("bKash") }
    var note by remember { mutableStateOf("") }

    // Installment fields
    var hasInstallments by remember { mutableStateOf(false) }
    var installmentAmountText by remember { mutableStateOf("") }
    var installmentFrequency by remember { mutableStateOf("Monthly") }
    var installmentCountText by remember { mutableStateOf("") }

    // Date picker helpers
    val calendar = Calendar.getInstance()

    val loanDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val c = Calendar.getInstance().apply { set(y, m, d) }
            loanDate = c.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val dueDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val c = Calendar.getInstance().apply { set(y, m, d) }
            dueDate = c.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Loan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_loan_back_button")
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
                .testTag("add_loan_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Direction Selector (I Lent Money vs I Borrowed Money)
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = direction == LoanDirection.LENT,
                        onClick = { direction = LoanDirection.LENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("add_loan_direction_lent")
                    ) {
                        Text("I Lent Money", fontWeight = FontWeight.SemiBold)
                    }
                    SegmentedButton(
                        selected = direction == LoanDirection.BORROWED,
                        onClick = { direction = LoanDirection.BORROWED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("add_loan_direction_borrowed")
                    ) {
                        Text("I Borrowed Money", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 2. Person Selector
            item {
                Column {
                    Text(
                        text = if (direction == LoanDirection.LENT) "Who did you give money to? *" else "Who lent you money? *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val selectedPerson = people.find { it.id == selectedPersonId }

                    ExposedDropdownMenuBox(
                        expanded = personDropdownExpanded,
                        onExpandedChange = { personDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedPerson?.name ?: "Select a person",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = personDropdownExpanded) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("add_loan_person_selector")
                        )

                        ExposedDropdownMenu(
                            expanded = personDropdownExpanded,
                            onDismissRequest = { personDropdownExpanded = false }
                        ) {
                            people.forEach { person ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(person.name, fontWeight = FontWeight.Medium)
                                            if (person.phone.isNotBlank()) {
                                                Text(person.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPersonId = person.id
                                        personDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("+ Add New Person", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    personDropdownExpanded = false
                                    showInlineAddPersonDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // 3. Amount Field
            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Loan Amount ($currency) *") },
                    placeholder = { Text("e.g. 15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError,
                    supportingText = if (amountError) { { Text("Please enter a valid amount greater than 0") } } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_loan_amount_input")
                )
            }

            // 4. Date Given & Due Date
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Loan Date
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { loanDatePicker.show() }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Date Given", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Formatters.formatDate(loanDate),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Due Date
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (hasDueDate) dueDatePicker.show()
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Due Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasDueDate && dueDate != null) Formatters.formatDate(dueDate) else "No fixed date",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Due Date presets & toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Has Promised Due Date", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hasDueDate,
                        onCheckedChange = {
                            hasDueDate = it
                            if (it && dueDate == null) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, 14)
                                dueDate = cal.timeInMillis
                            }
                        }
                    )
                }

                if (hasDueDate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                                    dueDate = c.timeInMillis
                                },
                                label = { Text("In 1 Week", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }
                                    dueDate = c.timeInMillis
                                },
                                label = { Text("In 2 Weeks", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
                                    dueDate = c.timeInMillis
                                },
                                label = { Text("In 1 Month", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }
                                    dueDate = c.timeInMillis
                                },
                                label = { Text("In 3 Months", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // 5. Purpose & Category
            item {
                Column {
                    Text(
                        text = "Purpose / Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LoanPurposeCategory.values()) { cat ->
                            FilterChip(
                                selected = purpose == cat.title,
                                onClick = { purpose = cat.title },
                                label = { Text(cat.title, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // 6. Payment Method
            item {
                Column {
                    Text(
                        text = "Payment Method Given Through",
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

            // 7. Installment Options (Optional)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Installment Repayment Plan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Switch(
                                checked = hasInstallments,
                                onCheckedChange = { hasInstallments = it }
                            )
                        }

                        if (hasInstallments) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = installmentAmountText,
                                    onValueChange = { installmentAmountText = it },
                                    label = { Text("Amount / installment") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = installmentCountText,
                                    onValueChange = { installmentCountText = it },
                                    label = { Text("Total installments") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            // 8. Notes
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Description") },
                    placeholder = { Text("e.g. Reason for loan, promised return terms, etc.") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_loan_notes_input")
                )
            }

            // 9. Save Button
            item {
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0) {
                            amountError = true
                            return@Button
                        }
                        val personId = selectedPersonId
                        if (personId == null) {
                            return@Button
                        }

                        val instAmount = installmentAmountText.toDoubleOrNull()
                        val instCount = installmentCountText.toIntOrNull()

                        viewModel.addLoan(
                            personId = personId,
                            direction = direction,
                            amount = parsedAmount,
                            loanDate = loanDate,
                            dueDate = if (hasDueDate) dueDate else null,
                            purpose = purpose,
                            note = note,
                            paymentMethod = paymentMethod,
                            installmentAmount = if (hasInstallments) instAmount else null,
                            installmentFrequency = if (hasInstallments) installmentFrequency else null,
                            installmentCount = if (hasInstallments) instCount else null,
                            onComplete = { newLoanId ->
                                onLoanCreated(newLoanId)
                            }
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("add_loan_submit_button")
                ) {
                    Text("Save Loan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showInlineAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showInlineAddPersonDialog = false },
            onConfirm = { name, phone, email, notes ->
                viewModel.addPerson(name, phone, email, notes) { newId ->
                    selectedPersonId = newId
                    showInlineAddPersonDialog = false
                }
            }
        )
    }
}
