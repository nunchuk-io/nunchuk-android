package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * "Cancel customization": restores an on-chain draft's default primary inheritance, keeping its
 * fallbacks, and returns the updated draft. Pass an empty/null [Param.groupId] for a personal draft.
 */
class ResetDraftPrimaryInheritanceUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<ResetDraftPrimaryInheritanceUseCase.Param, DraftWallet>(dispatcher) {

    override suspend fun execute(parameters: Param): DraftWallet {
        return repository.resetDraftPrimaryInheritance(groupId = parameters.groupId)
    }

    data class Param(val groupId: String? = null)
}
