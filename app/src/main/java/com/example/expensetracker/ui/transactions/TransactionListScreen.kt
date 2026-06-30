package com.example.expensetracker.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseEntity
import com.example.expensetracker.ui.components.getCategoryColor
import com.example.expensetracker.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    repository: ExpenseRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionListViewModel = viewModel { TransactionListViewModel(repository) }
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Search merchant or notes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            // Category filter row
            val categories = listOf("All", "Food", "Shopping", "Transport", "Bills", "Entertainment", "Others")
            Text(text = "Categories", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.setCategoryFilter(category) },
                        label = { Text(category) }
                    )
                }
            }

            // Source filter row
            val sources = listOf("All", "MANUAL", "SMS", "NOTIFICATION")
            Text(text = "Source", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(sources) { source ->
                    FilterChip(
                        selected = state.selectedSource == source,
                        onClick = { viewModel.setSourceFilter(source) },
                        label = { Text(source.capitalize()) }
                    )
                }
            }

            // Group transactions by date
            val groupedExpenses = remember(state.expenses) {
                val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                state.expenses.groupBy { dateFormat.format(Date(it.timestamp)) }
            }

            if (state.expenses.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "No matching transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedExpenses.forEach { (dateStr, list) ->
                        item {
                            Text(
                                text = dateStr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(list) { expense ->
                            TransactionItemRow(
                                expense = expense,
                                onClick = { editingExpense = expense },
                                onDelete = { viewModel.deleteExpense(expense) }
                            )
                        }
                    }
                }
            }
        }

        if (editingExpense != null) {
            EditExpenseDialog(
                expense = editingExpense!!,
                onDismiss = { editingExpense = null },
                onConfirm = { updated ->
                    viewModel.updateExpense(updated)
                    editingExpense = null
                }
            )
        }
    }
}

@Composable
fun TransactionItemRow(
    expense: ExpenseEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = expense.amount < 0.0
    val displayAmount = if (isIncome) -expense.amount else expense.amount
    val categoryColor = if (isIncome) Color(0xFF2E7D32) else getCategoryColor(expense.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = expense.merchant, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (!expense.note.isNullOrBlank()) {
                        Text(text = expense.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "Source: ${expense.source.capitalize()} ${if (expense.isVerified) "" else "(Unverified)"}",
                        fontSize = 10.sp,
                        color = if (expense.isVerified) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (isIncome) "+" else "-") + CurrencyFormatter.format(displayAmount, context),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onConfirm: (ExpenseEntity) -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf((if (expense.amount < 0.0) -expense.amount else expense.amount).toString()) }
    var category by remember { mutableStateOf(expense.category) }
    var merchant by remember { mutableStateOf(expense.merchant) }
    var note by remember { mutableStateOf(expense.note ?: "") }
    var isExpanded by remember { mutableStateOf(false) }

    val categories = if (expense.amount < 0.0) listOf("Income") else listOf("Food", "Shopping", "Transport", "Bills", "Entertainment", "Others")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (${CurrencyFormatter.getSymbol(context)})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Paid To") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        categories.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    category = selection
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (parsed != null && merchant.isNotBlank()) {
                        val isIncome = expense.amount < 0.0 || category == "Income"
                        onConfirm(
                            expense.copy(
                                amount = if (isIncome) -parsed else parsed,
                                category = category,
                                merchant = merchant,
                                note = note,
                                isVerified = true // Editing it confirms/verifies it
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
