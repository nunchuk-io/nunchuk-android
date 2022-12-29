package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetServerTransactionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: PremiumWalletRepository,
) : UseCase<GetServerTransactionUseCase.Param, ExtendedTransaction>(ioDispatcher) {
    override suspend fun execute(parameters: Param): ExtendedTransaction {
        return repository.getServerTransaction(parameters.walletId, parameters.txId)
    }

    data class Param(val walletId: String, val txId: String)
}