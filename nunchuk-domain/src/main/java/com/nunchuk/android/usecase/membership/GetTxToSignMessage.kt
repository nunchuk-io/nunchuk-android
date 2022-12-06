package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTxToSignMessage @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<GetTxToSignMessage.Param, String>(ioDispatcher) {

    override suspend fun execute(parameters: Param): String {
        return nativeSdk.getHealthCheckDummyTxMessage(parameters.walletId, parameters.userData)
    }

    data class Param(val walletId: String, val userData: String)
}