package com.nunchuk.android.repository

import com.nunchuk.android.model.payment.RecurringPayment

interface RecurringPaymentRepository {
    suspend fun getRecurringPayments(): List<RecurringPayment>
    suspend fun createRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPayment: RecurringPayment
    ): String
}