package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RemoveCoinFromTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<RemoveCoinFromTagUseCase.Param, Unit>(
    repository,
    nunchukNativeSdk,
    ioDispatcher
) {
    override suspend fun run(parameters: Param) {
        parameters.coins.forEach { output ->
            parameters.tagIds.forEach { tagId ->
                nunchukNativeSdk.removeFromCoinTag(
                    walletId = parameters.walletId,
                    txId = output.txid,
                    tagId = tagId,
                    vout = output.vout
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