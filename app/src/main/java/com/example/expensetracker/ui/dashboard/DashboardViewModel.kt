package com.example.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.BudgetEntity
import com.example.expensetracker.data.local.CategorySum
import com.example.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val categorySums: List<CategorySum>,
        val unverifiedExpenses: List<ExpenseEntity>,
        val recentExpenses: List<ExpenseEntity>,
        val weeklyAmounts: List<Double>,
        val budgets: List<BudgetEntity>,
        val totalMonthlySpend: Double,
        val totalMonthlyIncome: Double
    ) : DashboardUiState
}

class DashboardViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // Start/End of current month
    private val startOfMonth: Long
    private val endOfMonth: Long

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        endOfMonth = calendar.timeInMillis
    }

    // Monthly category sums
    private val categorySumsFlow = repository.getCategorySums(startOfMonth, endOfMonth)

    // Unverified transactions (waiting review)
    private val unverifiedFlow = repository.unverifiedExpenses

    // Recent transactions (limit to 5)
    private val recentExpensesFlow = repository.allExpenses.map { list -> list.take(5) }.flowOn(Dispatchers.IO)

    // Budgets
    private val budgetsFlow = repository.allBudgets

    // Monthly Income Flow
    private val monthlyIncomeFlow = repository.allExpenses.map { list ->
        list.filter { it.timestamp in startOfMonth..endOfMonth && it.amount < 0.0 }
            .sumOf { -it.amount }
    }.flowOn(Dispatchers.IO)

    // Weekly trend flow (calculate daily spending for last 7 days)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val weeklyTrendFlow = repository.allExpenses.flatMapLatest { expenses ->
        val calendar = Calendar.getInstance()
        val dailyAmounts = DoubleArray(7) { 0.0 }
        
        // Loop over the last 7 days
        for (i in 0..6) {
            val startOfDay = calendar.run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }
            val endOfDay = calendar.run {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
                timeInMillis
            }
            
            // Sum expenses for this day (amount > 0.0)
            val sum = expenses.filter { it.timestamp in startOfDay..endOfDay && it.amount > 0.0 }.sumOf { it.amount }
            dailyAmounts[6 - i] = sum
            
            // Go to previous day
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        flowOf(dailyAmounts.toList())
    }.flowOn(Dispatchers.IO)

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(categorySumsFlow, unverifiedFlow, recentExpensesFlow) { sums, unverified, recent ->
            Triple(sums, unverified, recent)
        },
        weeklyTrendFlow,
        budgetsFlow,
        monthlyIncomeFlow
    ) { triple, weekly, budgets, income ->
        val (sums, unverified, recent) = triple
        val monthlyTotal = sums.sumOf { it.total }
        DashboardUiState.Success(
            categorySums = sums,
            unverifiedExpenses = unverified,
            recentExpenses = recent,
            weeklyAmounts = weekly,
            budgets = budgets,
            totalMonthlySpend = monthlyTotal,
            totalMonthlyIncome = income
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    fun verifyExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense.copy(isVerified = true))
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addManualExpense(amount: Double, category: String, merchant: String, note: String) {
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseEntity(
                    amount = amount,
                    category = category,
                    merchant = merchant,
                    timestamp = System.currentTimeMillis(),
                    source = "MANUAL",
                    isVerified = true,
                    note = note
                )
            )
        }
    }

    fun seedMockData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()

                // Insert mock budgets
                repository.insertBudget(BudgetEntity("Food", 400.0))
                repository.insertBudget(BudgetEntity("Shopping", 300.0))
                repository.insertBudget(BudgetEntity("Transport", 150.0))
                repository.insertBudget(BudgetEntity("Bills", 500.0))

                val now = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L

                // Insert mock expenses
                val mockExpenses = listOf(
                    ExpenseEntity(amount = 15.50, category = "Food", merchant = "Starbucks", timestamp = now - 2 * 60 * 60 * 1000L, source = "MANUAL", isVerified = true, note = "Coffee & Croissant"),
                    ExpenseEntity(amount = 45.20, category = "Shopping", merchant = "Amazon", timestamp = now - 5 * 60 * 60 * 1000L, source = "SMS", isVerified = false, note = "Auto-captured from SMS"),
                    ExpenseEntity(amount = 8.00, category = "Transport", merchant = "Uber", timestamp = now - oneDayMs, source = "MANUAL", isVerified = true, note = "Commute to office"),
                    ExpenseEntity(amount = 120.00, category = "Bills", merchant = "Electric Utility", timestamp = now - oneDayMs - 2 * 60 * 60 * 1000L, source = "MANUAL", isVerified = true, note = "Monthly electricity bill"),
                    ExpenseEntity(amount = 35.00, category = "Food", merchant = "McDonalds", timestamp = now - 2 * oneDayMs, source = "NOTIFICATION", isVerified = false, note = "Auto-captured from notification"),
                    ExpenseEntity(amount = 89.99, category = "Shopping", merchant = "Target", timestamp = now - 2 * oneDayMs - 4 * 60 * 60 * 1000L, source = "MANUAL", isVerified = true, note = "Household supplies"),
                    ExpenseEntity(amount = 12.00, category = "Transport", merchant = "Gas Station", timestamp = now - 3 * oneDayMs, source = "MANUAL", isVerified = true, note = "Fuel refill"),
                    ExpenseEntity(amount = 25.40, category = "Entertainment", merchant = "Netflix", timestamp = now - 3 * oneDayMs - 6 * 60 * 60 * 1000L, source = "NOTIFICATION", isVerified = true, note = "Subscription"),
                    ExpenseEntity(amount = 6.50, category = "Food", merchant = "Vending Machine", timestamp = now - 4 * oneDayMs, source = "MANUAL", isVerified = true, note = "Snack purchase"),
                    ExpenseEntity(amount = 150.00, category = "Bills", merchant = "Rent Insurance", timestamp = now - 5 * oneDayMs, source = "MANUAL", isVerified = true, note = "Annual Premium")
                )

                mockExpenses.forEach {
                    repository.insertExpense(it)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
