package com.nunchuk.android.repository

import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.payment.RecurringPayment

interface RecurringPaymentRepository {
    suspend fun getRecurringPayments(
        groupId: String,
        walletId: String,
    ): List<RecurringPayment>
    suspend fun createRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPayment: RecurringPayment
    ): DummyTransactionPayload
    suspend fun deleteRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPaymentId: String,
    ): DummyTransactionPayload
    suspend fun getRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPaymentId: String,
    ): RecurringPayment
}