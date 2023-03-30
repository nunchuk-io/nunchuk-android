package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<DeleteCoinCollectionUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        nunchukNativeSdk.deleteCoinCollection(
            walletId = parameters.walletId,
            collectionId = parameters.collectionId,
        )
    }

    class Param(val walletId: String, val collectionId: Int)
}