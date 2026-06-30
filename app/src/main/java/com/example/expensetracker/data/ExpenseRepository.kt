package com.example.expensetracker.data

import android.content.Context
import com.example.expensetracker.data.local.AppDatabase
import com.example.expensetracker.data.local.BudgetEntity
import com.example.expensetracker.data.local.CategorySum
import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(context: Context) {
    private val expenseDao: ExpenseDao = AppDatabase.getDatabase(context).expenseDao

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()
    val unverifiedExpenses: Flow<List<ExpenseEntity>> = expenseDao.getUnverifiedExpensesFlow()
    val allBudgets: Flow<List<BudgetEntity>> = expenseDao.getAllBudgetsFlow()

    fun getExpensesForRange(start: Long, end: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesForRangeFlow(start, end)
    }

    suspend fun getExpensesForRangeList(start: Long, end: Long): List<ExpenseEntity> {
        return expenseDao.getExpensesForRange(start, end)
    }

    fun getCategorySums(start: Long, end: Long): Flow<List<CategorySum>> {
        return expenseDao.getCategorySumsFlow(start, end)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        if (expense.source == "SMS" || expense.source == "NOTIFICATION") {
            val duplicate = expenseDao.findDuplicateExpense(expense.amount, expense.timestamp, 180000)
            if (duplicate != null) {
                return duplicate.id
            }
        }
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        expenseDao.insertBudget(budget)
    }

    suspend fun deleteBudget(category: String) {
        expenseDao.deleteBudget(category)
    }

    suspend fun clearAllData() {
        expenseDao.deleteAllExpenses()
        expenseDao.deleteAllBudgets()
    }
}
