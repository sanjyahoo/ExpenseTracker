package com.example.expensetracker.utils

import android.content.Context
import android.telephony.TelephonyManager
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, context: Context): String {
        return try {
            val locale = getLocale(context)
            val format = NumberFormat.getCurrencyInstance(locale)
            format.format(amount)
        } catch (e: Exception) {
            "₹" + String.format("%.2f", amount) // Default fallback to Rupees
        }
    }

    fun getSymbol(context: Context): String {
        return try {
            val locale = getLocale(context)
            Currency.getInstance(locale).symbol
        } catch (e: Exception) {
            "₹"
        }
    }

    private fun getLocale(context: Context): Locale {
        val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val code = sharedPrefs.getString("currency_code", "AUTO") ?: "AUTO"
        
        return when (code) {
            "INR" -> Locale("en", "IN")
            "USD" -> Locale.US
            "EUR" -> Locale.FRANCE
            "GBP" -> Locale.UK
            else -> {
                val defaultLocale = Locale.getDefault()
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val simCountry = tm?.simCountryIso ?: ""
                val netCountry = tm?.networkCountryIso ?: ""
                
                if (simCountry.equals("IN", ignoreCase = true) || 
                    netCountry.equals("IN", ignoreCase = true) ||
                    defaultLocale.country.equals("IN", ignoreCase = true)) {
                    Locale("en", "IN")
                } else {
                    defaultLocale
                }
            }
        }
    }
}
