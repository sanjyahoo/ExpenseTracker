package com.example.expensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val merchant: String,
    val timestamp: Long,
    val source: String, // MANUAL, SMS, NOTIFICATION
    val isVerified: Boolean = true,
    val note: String? = null
)
