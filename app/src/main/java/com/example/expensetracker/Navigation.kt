package com.example.expensetracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.ui.budget.BudgetScreen
import com.example.expensetracker.ui.dashboard.DashboardScreen
import com.example.expensetracker.ui.income.IncomeScreen
import com.example.expensetracker.ui.settings.SettingsScreen
import com.example.expensetracker.ui.transactions.TransactionListScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val repository = remember { ExpenseRepository(context.applicationContext) }
    val backStack = rememberNavBackStack(DashboardKey)
    val currentKey = backStack.lastOrNull() ?: DashboardKey

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentKey == DashboardKey,
                    onClick = {
                        if (currentKey != DashboardKey) {
                            backStack.removeLastOrNull()
                            backStack.add(DashboardKey)
                        }
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentKey == TransactionsKey,
                    onClick = {
                        if (currentKey != TransactionsKey) {
                            backStack.removeLastOrNull()
                            backStack.add(TransactionsKey)
                        }
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
                    label = { Text("History") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentKey == IncomeKey,
                    onClick = {
                        if (currentKey != IncomeKey) {
                            backStack.removeLastOrNull()
                            backStack.add(IncomeKey)
                        }
                    },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Income") },
                    label = { Text("Income") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentKey == BudgetKey,
                    onClick = {
                        if (currentKey != BudgetKey) {
                            backStack.removeLastOrNull()
                            backStack.add(BudgetKey)
                        }
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Budget") },
                    label = { Text("Budget") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentKey == SettingsKey,
                    onClick = {
                        if (currentKey != SettingsKey) {
                            backStack.removeLastOrNull()
                            backStack.add(SettingsKey)
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    alwaysShowLabel = false
                )
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = { 
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            entryProvider = entryProvider {
                entry<DashboardKey> {
                    DashboardScreen(
                        repository = repository,
                        onNavigateToTransactions = {
                            backStack.removeLastOrNull()
                            backStack.add(TransactionsKey)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                entry<TransactionsKey> {
                    TransactionListScreen(
                        repository = repository,
                        onBackClick = {
                            backStack.removeLastOrNull()
                            backStack.add(DashboardKey)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                entry<IncomeKey> {
                    IncomeScreen(
                        repository = repository,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                entry<BudgetKey> {
                    BudgetScreen(
                        repository = repository,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                entry<SettingsKey> {
                    SettingsScreen(
                        repository = repository,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
