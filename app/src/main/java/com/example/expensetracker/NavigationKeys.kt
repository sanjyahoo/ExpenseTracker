package com.example.expensetracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object DashboardKey : NavKey
@Serializable data object TransactionsKey : NavKey
@Serializable data object IncomeKey : NavKey
@Serializable data object BudgetKey : NavKey
@Serializable data object SettingsKey : NavKey
