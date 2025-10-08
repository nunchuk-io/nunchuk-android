package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.WalletConfig
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
        repository.initWallet(parameters.walletConfig, parameters.groupId, parameters.walletType)
    }

    data class Param(
        val walletConfig: WalletConfig,
        val groupId: String? = null,
        val walletType: WalletType? = null
    )
}