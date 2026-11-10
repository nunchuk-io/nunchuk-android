package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Appends a fallback inheritance condition of any [Param.m] of [Param.n] keys to an on-chain draft
 * and returns the updated draft; its unlock time is set later. Pass an empty/null [Param.groupId]
 * for a personal draft.
 */
class AddDraftFallbackInheritanceUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<AddDraftFallbackInheritanceUseCase.Param, DraftWallet>(dispatcher) {

    override suspend fun execute(parameters: Param): DraftWallet {
        return repository.addDraftFallbackInheritance(
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
