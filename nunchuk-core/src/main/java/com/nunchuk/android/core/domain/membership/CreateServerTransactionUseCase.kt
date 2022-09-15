package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SeverWallet
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateServerTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<CreateServerTransactionUseCase.Params, Transaction?>(dispatcher) {
    override suspend fun execute(parameters: Params): Transaction? {
        return userWalletsRepository.createServerTransaction(
            walletId = parameters.walletId,
            psbt = parameters.psbt,
            note = parameters.note
        )
    }

    data class Params(val walletId: String, val psbt: String, val note: String?)
}