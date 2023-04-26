package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class IsMyCoinUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<IsMyCoinUseCase.Param, Boolean>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Boolean {
        return nativeSdk.isMyAddress(parameters.walletId, parameters.address)
    }

    data class Param(val walletId: String, val address: String,)
}