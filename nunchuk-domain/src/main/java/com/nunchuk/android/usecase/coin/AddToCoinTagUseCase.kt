package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddToCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<AddToCoinTagUseCase.Param, Unit>(
    repository,
    nunchukNativeSdk,
    ioDispatcher
) {
    override suspend fun run(parameters: Param) {
        parameters.coins.forEach { coin ->
            parameters.tagIds.forEach { tagId ->
                nunchukNativeSdk.addToCoinTag(
                    walletId = parameters.walletId,
                    txId = coin.txid,
                    tagId = tagId,
                    vout = coin.vout
                )
            }
        }
    }

    class Param(
        override val walletId: String,
        val tagIds: List<Int>,
        val coins: List<UnspentOutput>,
        override val isAssistedWallet: Boolean,
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}