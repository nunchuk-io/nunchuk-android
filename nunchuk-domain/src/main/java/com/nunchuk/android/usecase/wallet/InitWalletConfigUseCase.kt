package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class InitWalletConfigUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<InitWalletConfigUseCase.Param, Unit>(dispatcher) {

    override suspend fun execute(parameters: Param) {
        repository.initWallet(
            walletConfig = parameters.walletConfig,
            walletType = parameters.walletType,
            miniscriptTemplate = parameters.miniscriptTemplate,
            addressType = parameters.addressType,
            walletTemplate = parameters.walletTemplate,
            inheritancePolicyType = parameters.inheritancePolicyType,
        )
    }

    data class Param(
        val walletConfig: WalletConfig,
        val walletType: WalletType? = null,
        val miniscriptTemplate: String? = null,
        val addressType: String? = null,
        val walletTemplate: String? = null,
        /** On-chain only; null creates a legacy draft. */
        val inheritancePolicyType: OnChainInheritancePolicyType? = null,
    )
}