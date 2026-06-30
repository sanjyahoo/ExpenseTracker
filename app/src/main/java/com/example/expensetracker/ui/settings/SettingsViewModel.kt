package com.example.expensetracker.ui.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.BudgetEntity
import com.example.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

class SettingsViewModel(
    private val context: Context,
    private val repository: ExpenseRepository
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _isSmsCaptureEnabled = MutableStateFlow(sharedPrefs.getBoolean("sms_capture", true))
    val isSmsCaptureEnabled: StateFlow<Boolean> = _isSmsCaptureEnabled

    private val _isNotificationCaptureEnabled = MutableStateFlow(sharedPrefs.getBoolean("notification_capture", true))
    val isNotificationCaptureEnabled: StateFlow<Boolean> = _isNotificationCaptureEnabled

    private val _isBiometricLockEnabled = MutableStateFlow(sharedPrefs.getBoolean("biometric_lock", false))
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled

    private val _selectedCurrency = MutableStateFlow(sharedPrefs.getString("currency_code", "AUTO") ?: "AUTO")
    val selectedCurrency: StateFlow<String> = _selectedCurrency

    fun setSmsCapture(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("sms_capture", enabled).apply()
        _isSmsCaptureEnabled.value = enabled
    }

    fun setNotificationCapture(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("notification_capture", enabled).apply()
        _isNotificationCaptureEnabled.value = enabled
    }

    fun setBiometricLock(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("biometric_lock", enabled).apply()
        _isBiometricLockEnabled.value = enabled
    }

    fun setCurrencyCode(code: String) {
        sharedPrefs.edit().putString("currency_code", code).apply()
        _selectedCurrency.value = code
    }

    fun exportToCsv(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val expenses = repository.allExpenses.first()
                if (expenses.isEmpty()) {
                    onResult(false, "No expenses to export")
                    return@launch
                }

                // Save to downloads directory
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, "ExpenseTracker_Backup_${System.currentTimeMillis()}.csv")
                
                FileWriter(file).use { writer ->
                    // Header
                    writer.append("ID,Amount,Category,Merchant,Timestamp,Source,IsVerified,Note\n")
                    // Rows
                    expenses.forEach { e ->
                        writer.append("${e.id},${e.amount},${e.category},${e.merchant},${e.timestamp},${e.source},${e.isVerified},\"${(e.note ?: "").replace("\"", "\"\"")}\"\n")
                    }
                }
                
                onResult(true, "Successfully exported to: ${file.absolutePath}")
            } catch (e: Exception) {
                onResult(false, "Export failed: ${e.message}")
            }
        }
    }

    fun importFromCsv(csvContent: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = csvContent.lines()
                if (lines.size <= 1) {
                    onResult(false, "Empty or invalid CSV file")
                    return@launch
                }

                var importCount = 0
                // Skip header line
                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isBlank()) continue

                    // Parse CSV line (simple split, handling quoted notes)
                    val tokens = parseCsvLine(line)
                    if (tokens.size >= 7) {
                        val amount = tokens[1].toDoubleOrNull() ?: continue
                        val category = tokens[2]
                        val merchant = tokens[3]
                        val timestamp = tokens[4].toLongOrNull() ?: System.currentTimeMillis()
                        val source = tokens[5]
                        val isVerified = tokens[6].toBoolean()
                        val note = if (tokens.size >= 8) tokens[7] else ""

                        repository.insertExpense(
                            ExpenseEntity(
                                amount = amount,
                                category = category,
                                merchant = merchant,
                                timestamp = timestamp,
                                source = source,
                                isVerified = isVerified,
                                note = note.ifBlank { null }
                            )
                        )
                        importCount++
                    }
                }
                onResult(true, "Successfully imported $importCount transactions")
            } catch (e: Exception) {
                onResult(false, "Import failed: ${e.message}")
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        var currentToken = StringBuilder()
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(currentToken.toString().trim())
                    currentToken = StringBuilder()
                }
                else -> currentToken.append(char)
            }
        }
        result.add(currentToken.toString().trim())
        return result
    }

    fun clearAllData(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                onResult(true, "All transactions and budgets cleared")
            } catch (e: Exception) {
                onResult(false, "Clear failed: ${e.message}")
            }
        }
    }

    fun seedDummyData(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // Clear existing first to ensure clean state
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

                onResult(true, "Successfully seeded mock data!")
            } catch (e: Exception) {
                onResult(false, "Seeding failed: ${e.message}")
            }
        }
    }
}
