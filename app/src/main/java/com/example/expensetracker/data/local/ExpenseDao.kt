package com.example.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategorySum(
    val category: String,
    val total: Double
)

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getExpensesForRangeFlow(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    suspend fun getExpensesForRange(start: Long, end: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE isVerified = 0 ORDER BY timestamp DESC")
    fun getUnverifiedExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE timestamp >= :start AND timestamp <= :end AND amount > 0.0 GROUP BY category")
    fun getCategorySumsFlow(start: Long, end: Long): Flow<List<CategorySum>>

    // Budgets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budgets")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Query("SELECT * FROM expenses WHERE abs(amount - :amount) < 0.001 AND abs(timestamp - :timestamp) <= :timeWindowMs LIMIT 1")
    suspend fun findDuplicateExpense(amount: Double, timestamp: Long, timeWindowMs: Long): ExpenseEntity?

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
