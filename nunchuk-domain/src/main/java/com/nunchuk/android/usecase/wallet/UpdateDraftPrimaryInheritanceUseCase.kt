package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Customizes an on-chain draft's primary inheritance to any [Param.m] of [Param.n] keys and returns
 * the updated draft. Pass an empty/null [Param.groupId] for a personal draft.
 */
class UpdateDraftPrimaryInheritanceUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<UpdateDraftPrimaryInheritanceUseCase.Param, DraftWallet>(dispatcher) {

    override suspend fun execute(parameters: Param): DraftWallet {
        return repository.updateDraftPrimaryInheritance(
            groupId = parameters.groupId,
            m = parameters.m,
            n = parameters.n,
        )
    }

    data class Param(
        val m: Int,
        val n: Int,
        val groupId: String? = null,
    )
}
