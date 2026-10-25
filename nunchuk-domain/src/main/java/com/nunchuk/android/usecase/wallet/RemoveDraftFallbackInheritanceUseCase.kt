package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Removes the zero-based fallback [Param.index] of an on-chain draft; later fallbacks move down one
 * and keep their keys and unlock times. Returns the updated draft.
 */
class RemoveDraftFallbackInheritanceUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<RemoveDraftFallbackInheritanceUseCase.Param, DraftWallet>(dispatcher) {

    override suspend fun execute(parameters: Param): DraftWallet {
        return repository.removeDraftFallbackInheritance(
            groupId = parameters.groupId,
            index = parameters.index,
        )
    }

    data class Param(
        val index: Int,
        val groupId: String? = null,
    )
}
