package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.model.byzantine.toDomainModel
import com.nunchuk.android.core.data.model.payment.CreateRecurringPaymentRequest
import com.nunchuk.android.core.data.model.payment.toModel
import com.nunchuk.android.core.data.model.payment.toRequest
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.repository.RecurringPaymentRepository
import javax.inject.Inject

internal class RecurringPaymentRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val userWalletRepository: PremiumWalletRepository,
) : RecurringPaymentRepository {
    override suspend fun getRecurringPayments(
        groupId: String,
        walletId: String,
    ): List<RecurringPayment> {
        val response = userWalletApiManager.groupWalletApi.getRecurringPayments(
            groupId = groupId,
            walletId = walletId
        )
        return response.data.recurringPayments.map { it.toModel() }
    }

    override suspend fun createRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPayment: RecurringPayment,
    ): DummyTransactionPayload {
        val nonce = userWalletRepository.getNonce()
        val request = recurringPayment.toRequest(nonce)
        val response = userWalletApiManager.groupWalletApi.createRecurringPayment(
            groupId = groupId,
            walletId = walletId,
            request = request
        )
        return response.data.dummyTransaction?.toDomainModel()
            ?: throw IllegalStateException("Dummy transaction is null")
    }

    override suspend fun deleteRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPaymentId: String,
    ): DummyTransactionPayload {
        val nonce = userWalletRepository.getNonce()
        return userWalletApiManager.groupWalletApi.deleteRecurringPayment(
            groupId = groupId,
            walletId = walletId,
            recurringPaymentId = recurringPaymentId,
            request = CreateRecurringPaymentRequest(nonce)
        ).data.dummyTransaction?.toDomainModel()
            ?: throw IllegalStateException("Dummy transaction is null")
    }

    override suspend fun getRecurringPayment(
        groupId: String,
        walletId: String,
        recurringPaymentId: String,
    ): RecurringPayment {
        val response = userWalletApiManager.groupWalletApi.getRecurringPayment(
            groupId = groupId,
            walletId = walletId,
            recurringPaymentId = recurringPaymentId
        )
        return response.data.recurringPayment?.toModel()
            ?: throw IllegalStateException("Recurring payment is null")
    }
}