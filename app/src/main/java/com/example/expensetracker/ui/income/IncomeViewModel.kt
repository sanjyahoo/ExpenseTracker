package com.example.expensetracker.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface IncomeUiState {
    object Loading : IncomeUiState
    data class Success(
        val incomeList: List<ExpenseEntity>,
        val totalMonthlyIncome: Double
    ) : IncomeUiState
}

class IncomeViewModel(private val repository: ExpenseRepository) : ViewModel() {

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

    val uiState: StateFlow<IncomeUiState> = repository.allExpenses.map { list ->
        val incomes = list.filter { it.amount < 0.0 }
        val monthlyIncome = incomes.filter { it.timestamp in startOfMonth..endOfMonth }.sumOf { -it.amount }
        IncomeUiState.Success(
            incomeList = incomes,
            totalMonthlyIncome = monthlyIncome
        )
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IncomeUiState.Loading
    )

    fun addManualIncome(amount: Double, source: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val income = ExpenseEntity(
                amount = -amount, // Stored as negative for income
                category = "Income",
                merchant = source,
                timestamp = System.currentTimeMillis(),
                source = "MANUAL",
                isVerified = true,
                note = note.ifBlank { "Manual Income: $source" }
            )
            repository.insertExpense(income)
        }
    }

    fun deleteIncome(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }
}
