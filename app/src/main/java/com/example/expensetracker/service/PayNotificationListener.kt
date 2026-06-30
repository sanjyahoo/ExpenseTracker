package com.example.expensetracker.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseEntity
import com.example.expensetracker.receiver.SmsReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PayNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("PayNotificationListener", "Notification Listener connected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: ""
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        Log.d("PayNotificationListener", "Notification from $packageName: Title: $title, Text: $text")

        // Filter and check if this notification is transaction related
        if (isPaymentAppOrKeyword(packageName, title, text)) {
            val contentToParse = "$title $text"
            val parseResult = SmsReceiver.parseTransaction(contentToParse)
            if (parseResult != null) {
                val repository = ExpenseRepository(applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val isCredit = parseResult.type == SmsReceiver.Companion.TransactionType.CREDIT
                    val signedAmount = if (isCredit) -parseResult.amount else parseResult.amount
                    val category = if (isCredit) "Income" else parseResult.category
                    val note = if (isCredit) {
                        "Auto-captured Income from notification ($title: $text)"
                    } else {
                        "Auto-captured from notification ($title: $text)"
                    }

                    val expense = ExpenseEntity(
                        amount = signedAmount,
                        category = category,
                        merchant = parseResult.merchant.ifBlank { getAppNameFromPackage(packageName) },
                        timestamp = System.currentTimeMillis(),
                        source = "NOTIFICATION",
                        isVerified = false,
                        note = note
                    )
                    repository.insertExpense(expense)
                    Log.d("PayNotificationListener", "Successfully logged transaction of $signedAmount from notification")
                }
            }
        }
    }

    private fun isPaymentAppOrKeyword(packageName: String, title: String, text: String): Boolean {
        val lowerText = "$title $text".lowercase()
        // Known payment package lists (Google Pay, PhonePe, bank apps, payment alerts)
        val isPaymentApp = packageName.contains("wallet") || 
                           packageName.contains("pay") || 
                           packageName.contains("bank") || 
                           packageName.contains("finance") || 
                           packageName.contains("card") ||
                           packageName.contains("paisa") ||
                           packageName.contains("phonepe")
        
        // Transaction indicator keywords
        val hasTransactionKeywords = lowerText.contains("debited") || 
                                     lowerText.contains("spent") || 
                                     lowerText.contains("withdrawn") || 
                                     lowerText.contains("paid") || 
                                     lowerText.contains("charged") ||
                                     lowerText.contains("sent") ||
                                     lowerText.contains("transfer")
        
        return isPaymentApp || hasTransactionKeywords
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName.split(".").last().capitalize()
        }
    }
}
