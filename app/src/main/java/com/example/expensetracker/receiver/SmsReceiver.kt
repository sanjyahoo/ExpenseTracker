package com.example.expensetracker.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.expensetracker.BuildConfig
import com.example.expensetracker.MainActivity
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.ExpenseEntity
import com.example.expensetracker.utils.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        // Respect the user's SMS auto-capture toggle
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sms_capture", true)) return

        val bundle = intent.extras ?: return
        try {
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")
            for (pdu in pdus) {
                val bytePdu = pdu as? ByteArray ?: continue
                val message = SmsMessage.createFromPdu(bytePdu, format) ?: continue
                val body = message.messageBody ?: continue
                val sender = message.originatingAddress ?: "Unknown"

                // Only log in debug builds — SMS may contain OTPs and account details
                if (BuildConfig.DEBUG) {
                    Log.d("SmsReceiver", "SMS Received from $sender")
                }
                processSms(context, sender, body)
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS", e)
        }
    }

    private fun processSms(context: Context, sender: String, body: String) {
        val parseResult = parseTransaction(body) ?: return

        val repository = ExpenseRepository(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val signedAmount = if (parseResult.type == TransactionType.CREDIT) -parseResult.amount else parseResult.amount
            val category = if (parseResult.type == TransactionType.CREDIT) "Income" else parseResult.category
            val note = if (parseResult.type == TransactionType.CREDIT) {
                "Auto-captured Income from SMS: \"${body.take(60)}...\""
            } else {
                "Auto-captured from SMS: \"${body.take(60)}...\""
            }

            val expense = ExpenseEntity(
                amount = signedAmount,
                category = category,
                merchant = parseResult.merchant.ifBlank { sender },
                timestamp = System.currentTimeMillis(),
                source = "SMS",
                isVerified = false,
                note = note
            )
            repository.insertExpense(expense)
            showNotification(context, signedAmount, parseResult.merchant.ifBlank { sender })
        }
    }

    private fun showNotification(context: Context, amount: Double, merchant: String) {
        val channelId = "expense_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for auto-captured transactions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isIncome = amount < 0.0
        val displayAmount = if (isIncome) -amount else amount
        val title = if (isIncome) "New Income Captured" else "New Expense Captured"
        val content = if (isIncome) {
            "Received " + CurrencyFormatter.format(displayAmount, context) + " from $merchant. Tap to verify."
        } else {
            "Spent " + CurrencyFormatter.format(displayAmount, context) + " at $merchant. Tap to verify."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        enum class TransactionType { DEBIT, CREDIT }
        data class ParseResult(val amount: Double, val type: TransactionType, val merchant: String, val category: String)

        // Common regex patterns for amount extraction
        // E.g. Rs 500, Rs.500, INR 500, $500, USD 500, 500.00 spent
        private val amountPatterns = listOf(
            Pattern.compile("(?i)(?:rs\\.?|inr|usd|\\$)\\s*([\\d,]+\\.?\\d*)"),
            Pattern.compile("(?i)([\\d,]+\\.?\\d*)\\s*(?:rs\\.?|inr|usd|\\$)"),
            Pattern.compile("(?i)(?:debited|spent|withdrawn|charged|paid)\\s+(?:of\\s+)?([\\d,]+\\.?\\d*)")
        )

        private val debitKeywords = listOf("debited", "spent", "charged", "withdrawn", "paid", "purchased", "sent")
        private val creditKeywords = listOf("credited", "refunded", "deposited", "received", "added")

        fun parseTransaction(body: String): ParseResult? {
            val lowerBody = body.lowercase()
            
            // Determine transaction type
            val isDebit = debitKeywords.any { lowerBody.contains(it) }
            val isCredit = creditKeywords.any { lowerBody.contains(it) }
            
            if (!isDebit && !isCredit) return null
            val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

            // Extract amount
            var amount = 0.0
            for (pattern in amountPatterns) {
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                    val parsed = amountStr.toDoubleOrNull()
                    if (parsed != null) {
                        amount = parsed
                        break
                    }
                }
            }
            if (amount <= 0.0) return null

            // Extract merchant
            var merchant = ""
            val merchantPatterns = listOf(
                Pattern.compile("(?i)(?:at|to|on)\\s+([a-zA-Z0-9\\s]{3,20})"),
                Pattern.compile("(?i)(?:spent\\s+on|paid\\s+to)\\s+([a-zA-Z0-9\\s]{3,20})")
            )
            for (pattern in merchantPatterns) {
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    merchant = matcher.group(1)?.trim() ?: ""
                    break
                }
            }

            // Inferred category from merchant
            val category = inferCategory(merchant)

            return ParseResult(amount, type, merchant, category)
        }

        private fun inferCategory(merchant: String): String {
            val m = merchant.lowercase()
            return when {
                m.contains("uber") || m.contains("lyft") || m.contains("cab") || m.contains("metro") || m.contains("train") || m.contains("bus") -> "Transport"
                m.contains("starbucks") || m.contains("mcdonald") || m.contains("cafe") || m.contains("restaurant") || m.contains("food") || m.contains("grill") -> "Food"
                m.contains("amazon") || m.contains("walmart") || m.contains("target") || m.contains("grocery") || m.contains("supermarket") || m.contains("mart") -> "Shopping"
                m.contains("netflix") || m.contains("spotify") || m.contains("steam") || m.contains("cinema") || m.contains("theatre") || m.contains("google play") -> "Entertainment"
                m.contains("electricity") || m.contains("water") || m.contains("gas") || m.contains("internet") || m.contains("phone") || m.contains("mobile") || m.contains("telecom") -> "Bills"
                else -> "Uncategorized"
            }
        }
    }
}
