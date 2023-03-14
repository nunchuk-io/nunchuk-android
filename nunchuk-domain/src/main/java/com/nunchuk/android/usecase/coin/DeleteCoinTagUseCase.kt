package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<DeleteCoinTagUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        nunchukNativeSdk.deleteCoinTag(
            walletId = parameters.walletId,
            tagId = parameters.tagId,
        )
    }

    class Param(val walletId: String, val tagId: Int)
}