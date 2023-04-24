package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetListCoinByTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<GetListCoinByTagUseCase.Param, List<UnspentOutput>>(ioDispatcher) {
    override suspend fun execute(parameters: Param): List<UnspentOutput> {
        return nunchukNativeSdk.getCoinByTag(
            walletId = parameters.walletId,
            tagId = parameters.tagId
        )
    }

    class Param(val walletId: String, val tagId: Int)
}