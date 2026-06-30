package com.example.expensetracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionListUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val selectedSource: String = "All" // All, MANUAL, SMS, NOTIFICATION
)

class TransactionListViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val categoryFilter = MutableStateFlow("All")
    private val searchQueryFilter = MutableStateFlow("")
    private val sourceFilter = MutableStateFlow("All")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionListUiState> = combine(
        categoryFilter,
        searchQueryFilter,
        sourceFilter
    ) { category, search, source ->
        Triple(category, search, source)
    }.flatMapLatest { (category, search, source) ->
        repository.allExpenses.map { list ->
            val filtered = list.filter { expense ->
                val matchesCategory = category == "All" || expense.category == category
                val matchesSource = source == "All" || expense.source == source
                val matchesSearch = search.isBlank() || 
                                    expense.merchant.contains(search, ignoreCase = true) || 
                                    (expense.note ?: "").contains(search, ignoreCase = true)
                matchesCategory && matchesSource && matchesSearch
            }
            TransactionListUiState(
                expenses = filtered,
                selectedCategory = category,
                searchQuery = search,
                selectedSource = source
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionListUiState()
    )

    fun setCategoryFilter(category: String) {
        categoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        searchQueryFilter.value = query
    }

    fun setSourceFilter(source: String) {
        sourceFilter.value = source
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }
}
