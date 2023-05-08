package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetCoinAncestryUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<GetCoinAncestryUseCase.Param, List<List<UnspentOutput>>>(dispatcher) {
    override suspend fun execute(parameters: Param): List<List<UnspentOutput>> {
        return nunchukNativeSdk.getCoinAncestry(parameters.walletId, parameters.txId, parameters.vout)
    }

    data class Param(val walletId: String, val txId: String, val vout: Int)
}