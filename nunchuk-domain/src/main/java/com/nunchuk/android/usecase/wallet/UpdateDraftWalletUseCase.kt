package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Updates the current draft wallet via PUT /draft-wallets/current.
 * Used by custom Miniscript setup. Pass an empty/null [Param.groupId] for personal
 * draft wallets. [Param.platformKeySlots] currently allows a single slot.
 */
class UpdateDraftWalletUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<UpdateDraftWalletUseCase.Param, DraftWallet>(dispatcher) {

    override suspend fun execute(parameters: Param): DraftWallet {
        return repository.updateDraftWallet(
            groupId = parameters.groupId,
            walletConfig = parameters.walletConfig,
            walletType = parameters.walletType,
            miniscriptTemplate = parameters.miniscriptTemplate,
            platformKeySlots = parameters.platformKeySlots,
            addressType = parameters.addressType,
            walletTemplate = parameters.walletTemplate,
        )
    }

    data class Param(
        val walletConfig: WalletConfig,
        val groupId: String? = null,
        val walletType: WalletType? = null,
        val miniscriptTemplate: String? = null,
        val platformKeySlots: List<String> = emptyList(),
        val addressType: String? = null,
        val walletTemplate: String? = null,
    )
}
