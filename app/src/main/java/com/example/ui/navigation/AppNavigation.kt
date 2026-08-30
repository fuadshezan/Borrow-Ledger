package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AppThemeMode
import com.example.data.model.PaymentMethodOption
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.loans.AddLoanScreen
import com.example.ui.screens.loans.LoanDetailScreen
import com.example.ui.screens.loans.LoansLedgerScreen
import com.example.ui.screens.payments.AddPaymentScreen
import com.example.ui.screens.people.PeopleListScreen
import com.example.ui.screens.people.PersonDetailScreen
import com.example.ui.screens.reminders.RemindersScreen
import com.example.ui.screens.search.GlobalSearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.whoowes.WhoOwesMeScreen
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.LendingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object People : Screen("people", "People", Icons.Default.People)
    data object Loans : Screen("loans", "Ledger", Icons.Default.ReceiptLong)
    data object WhoOwesMe : Screen("who_owes_me", "Who Owes", Icons.Default.CallReceived)
    data object Reminders : Screen("reminders", "Reminders", Icons.Default.Notifications)

    data object PersonDetail : Screen("person_detail/{personId}", "Person Detail", Icons.Default.People) {
        fun createRoute(personId: Long) = "person_detail/$personId"
    }

    data object LoanDetail : Screen("loan_detail/{loanId}", "Loan Detail", Icons.Default.ReceiptLong) {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }

    data object AddLoan : Screen("add_loan?personId={personId}", "New Loan", Icons.Default.Add) {
        fun createRoute(personId: Long? = null) = if (personId != null) "add_loan?personId=$personId" else "add_loan"
    }

    data object AddPayment : Screen("add_payment?loanId={loanId}", "Record Payment", Icons.Default.AttachMoney) {
        fun createRoute(loanId: Long? = null) = if (loanId != null) "add_payment?loanId=$loanId" else "add_payment"
    }

    data object GlobalSearch : Screen("search", "Search", Icons.Default.Search)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.People,
    Screen.Loans,
    Screen.WhoOwesMe,
    Screen.Reminders
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: LendingViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val reminders by viewModel.reminderItems.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val pendingRemindersCount = reminders.count { !it.isCompleted }

    var showQuickAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isTopLevelDestination) {
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                val isDark = AppTheme.colors.isDark

                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppTheme.colors.iconBoxBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currency,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppTheme.colors.iconBoxTint,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Lending Tracker",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleThemeMode() },
                            modifier = Modifier.testTag("appbar_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { navController.navigate(Screen.GlobalSearch.route) },
                            modifier = Modifier.testTag("appbar_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { navController.navigate(Screen.Settings.route) },
                            modifier = Modifier.testTag("appbar_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.cardBorder)
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Dashboard.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    if (screen == Screen.Reminders && pendingRemindersCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = AppTheme.colors.redText,
                                                    contentColor = Color.White
                                                ) { Text("$pendingRemindersCount") }
                                            }
                                        ) {
                                            Icon(screen.icon, contentDescription = screen.title)
                                        }
                                    } else {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                },
                                label = { Text(screen.title, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("bottom_nav_${screen.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToLoanDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    },
                    onNavigateToPersonDetail = { personId ->
                        navController.navigate(Screen.PersonDetail.createRoute(personId))
                    },
                    onNavigateToAddLoan = {
                        navController.navigate(Screen.AddLoan.createRoute())
                    },
                    onNavigateToAddPayment = { loanId ->
                        navController.navigate(Screen.AddPayment.createRoute(loanId))
                    },
                    onNavigateToAddPerson = {
                        navController.navigate(Screen.People.route)
                    },
                    onNavigateToWhoOwesMe = {
                        navController.navigate(Screen.WhoOwesMe.route)
                    },
                    onNavigateToReminders = {
                        navController.navigate(Screen.Reminders.route)
                    },
                    onNavigateToLoans = {
                        navController.navigate(Screen.Loans.route)
                    },
                    onOpenQuickAdd = {
                        showQuickAddSheet = true
                    }
                )
            }

            composable(Screen.People.route) {
                PeopleListScreen(
                    viewModel = viewModel,
                    onNavigateToPersonDetail = { personId ->
                        navController.navigate(Screen.PersonDetail.createRoute(personId))
                    }
                )
            }

            composable(Screen.Loans.route) {
                LoansLedgerScreen(
                    viewModel = viewModel,
                    onNavigateToLoanDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    },
                    onNavigateToAddLoan = {
                        navController.navigate(Screen.AddLoan.createRoute())
                    },
                    onNavigateToAddPayment = { loanId ->
                        navController.navigate(Screen.AddPayment.createRoute(loanId))
                    }
                )
            }

            composable(Screen.WhoOwesMe.route) {
                WhoOwesMeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPersonDetail = { personId ->
                        navController.navigate(Screen.PersonDetail.createRoute(personId))
                    },
                    onNavigateToAddLoan = {
                        navController.navigate(Screen.AddLoan.createRoute())
                    }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLoanDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    },
                    onNavigateToAddPayment = { loanId ->
                        navController.navigate(Screen.AddPayment.createRoute(loanId))
                    }
                )
            }

            composable(
                route = Screen.PersonDetail.route,
                arguments = listOf(navArgument("personId") { type = NavType.LongType })
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId") ?: 0L
                PersonDetailScreen(
                    personId = personId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLoanDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    },
                    onNavigateToAddLoanForPerson = { pId ->
                        navController.navigate(Screen.AddLoan.createRoute(pId))
                    },
                    onNavigateToAddPaymentForLoan = { loanId ->
                        navController.navigate(Screen.AddPayment.createRoute(loanId))
                    }
                )
            }

            composable(
                route = Screen.LoanDetail.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                LoanDetailScreen(
                    loanId = loanId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPersonDetail = { personId ->
                        navController.navigate(Screen.PersonDetail.createRoute(personId))
                    },
                    onNavigateToAddPayment = { lId ->
                        navController.navigate(Screen.AddPayment.createRoute(lId))
                    }
                )
            }

            composable(
                route = Screen.AddLoan.route,
                arguments = listOf(navArgument("personId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val pId = backStackEntry.arguments?.getLong("personId")
                val personId = if (pId != null && pId > 0) pId else null
                AddLoanScreen(
                    initialPersonId = personId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onLoanCreated = { newLoanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(newLoanId)) {
                            popUpTo(Screen.Loans.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.AddPayment.route,
                arguments = listOf(navArgument("loanId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val lId = backStackEntry.arguments?.getLong("loanId")
                val loanId = if (lId != null && lId > 0) lId else null
                AddPaymentScreen(
                    initialLoanId = loanId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPaymentSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.GlobalSearch.route) {
                GlobalSearchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPersonDetail = { personId ->
                        navController.navigate(Screen.PersonDetail.createRoute(personId))
                    },
                    onNavigateToLoanDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    // 5-Second Quick Add Bottom Sheet
    if (showQuickAddSheet) {
        QuickAddBottomSheet(
            viewModel = viewModel,
            currencySymbol = currency,
            onDismiss = { showQuickAddSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddBottomSheet(
    viewModel: LendingViewModel,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var personName by remember { mutableStateOf("") }
    var isGaveMoney by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("bKash") }
    var purpose by remember { mutableStateOf("Quick Loan") }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("quick_add_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ 5-Second Quick Record",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Relationship direction toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isGaveMoney,
                    onClick = { isGaveMoney = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("I Gave Money", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                SegmentedButton(
                    selected = !isGaveMoney,
                    onClick = { isGaveMoney = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("I Received / Borrowed", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // Person Name
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    errorText = null
                },
                label = { Text("Person Name *") },
                placeholder = { Text("e.g. Rahim, Karim, Sakib") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_person_input")
            )

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorText = null
                },
                label = { Text("Amount ($currencySymbol) *") },
                placeholder = { Text("e.g. 5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_amount_input")
            )

            // Payment Method Quick Chips
            Column {
                Text(
                    text = "Given Via",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("bKash", "Cash", "Nagad", "Bank")) { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method, fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (personName.isBlank()) {
                        errorText = "Please enter the person's name"
                        return@Button
                    }
                    if (amt == null || amt <= 0) {
                        errorText = "Please enter a valid amount"
                        return@Button
                    }

                    viewModel.quickAdd(
                        personName = personName,
                        isGaveMoney = isGaveMoney,
                        amount = amt,
                        date = System.currentTimeMillis(),
                        paymentMethod = paymentMethod,
                        purpose = purpose,
                        note = "Quick entry",
                        onComplete = onDismiss
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("quick_add_submit_button")
            ) {
                Text("Save in 1-Tap", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
