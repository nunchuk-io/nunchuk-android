package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ReplaceServerTransactionUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<ReplaceServerTransactionUseCase.Params, Unit>(dispatcher) {

    override suspend fun execute(parameters: Params) {
        repository.replaceTransaction(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            transactionId = parameters.transactionId,
            newTxPsbt = parameters.newTxPsbt,
        )
    }

    data class Params(
        val groupId: String?,
        val walletId: String,
        val transactionId: String,
        val newTxPsbt: String,
    )
}