package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetAddressPathUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<GetAddressPathUseCase.Params, String>(ioDispatcher) {

    override suspend fun execute(parameters: Params): String {
        return nunchukNativeSdk.getAddressPath(parameters.walletId, parameters.address)
    }

    class Params(val walletId: String, val address: String)
}
