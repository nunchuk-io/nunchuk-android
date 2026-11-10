package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Edits the zero-based fallback [Param.index] of an on-chain draft: its M of N and/or its unlock
 * time. A null field is left unchanged. Returns the updated draft.
 */
class UpdateDraftFallbackInheritanceUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<UpdateDraftFallbackInheritanceUseCase.Param, DraftWallet>(dispatcher) {

    override suspend fun execute(parameters: Param): DraftWallet {
        return repository.updateDraftFallbackInheritance(
            groupId = parameters.groupId,
            index = parameters.index,
            m = parameters.m,
            n = parameters.n,
            timelock = parameters.timelock,
        )
    }

    data class Param(
        val index: Int,
        val m: Int? = null,
        val n: Int? = null,
        val timelock: WalletTimelock? = null,
        val groupId: String? = null,
    )
}
