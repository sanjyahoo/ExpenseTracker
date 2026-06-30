package com.example.expensetracker

import com.example.expensetracker.receiver.SmsReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsReceiverTest {

    @Test
    fun testParseDebitTransaction() {
        val sms = "Your account XX123 has been debited Rs. 500.00 at Amazon India. Remaining balance is Rs. 10000."
        val result = SmsReceiver.parseTransaction(sms)
        
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals(SmsReceiver.Companion.TransactionType.DEBIT, result.type)
        assertEquals("Amazon India", result.merchant)
        assertEquals("Shopping", result.category)
    }

    @Test
    fun testParseDollarSpent() {
        val sms = "Thank you for using your card. Spent $24.99 at Starbucks. Call if not done by you."
        val result = SmsReceiver.parseTransaction(sms)
        
        assertNotNull(result)
        assertEquals(24.99, result!!.amount, 0.001)
        assertEquals(SmsReceiver.Companion.TransactionType.DEBIT, result.type)
        assertEquals("Starbucks", result.merchant)
        assertEquals("Food", result.category)
    }

    @Test
    fun testParseUberRide() {
        val sms = "Paid $15.50 to Uber Ride. Your receipt will be sent shortly."
        val result = SmsReceiver.parseTransaction(sms)
        
        assertNotNull(result)
        assertEquals(15.50, result!!.amount, 0.001)
        assertEquals(SmsReceiver.Companion.TransactionType.DEBIT, result.type)
        assertEquals("Uber Ride", result.merchant)
        assertEquals("Transport", result.category)
    }

    @Test
    fun testIgnoreCreditTransaction() {
        val sms = "Your account XX123 has been credited INR 12,000.00. Salary deposit."
        val result = SmsReceiver.parseTransaction(sms)
        
        assertNotNull(result)
        assertEquals(12000.0, result!!.amount, 0.001)
        assertEquals(SmsReceiver.Companion.TransactionType.CREDIT, result.type)
    }

    @Test
    fun testNonTransactionSms() {
        val sms = "Hey, are we still meeting today at 5 PM?"
        val result = SmsReceiver.parseTransaction(sms)
        
        assertNull(result)
    }
}
