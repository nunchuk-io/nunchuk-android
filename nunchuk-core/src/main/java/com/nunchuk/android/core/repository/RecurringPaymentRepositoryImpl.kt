package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.model.payment.toRequest
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.repository.RecurringPaymentRepository
import javax.inject.Inject

internal class RecurringPaymentRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val userWalletRepository: PremiumWalletRepository,
) : RecurringPaymentRepository {
    override suspend fun getRecurringPayments(): List<RecurringPayment> {
        TODO("Not yet implemented")
    }

    override suspend fun createRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPayment: RecurringPayment
    ): String {
        val nonce = userWalletRepository.getNonce()
        val request = recurringPayment.toRequest(nonce)
        val response = userWalletApiManager.groupWalletApi.createRecurringPayment(groupId, walletId, request)
        return response.data.dummyTransaction?.id ?: throw IllegalStateException("Dummy transaction id is null")
    }
}