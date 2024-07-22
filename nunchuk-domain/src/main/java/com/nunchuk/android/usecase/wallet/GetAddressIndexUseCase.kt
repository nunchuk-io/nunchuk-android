package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetAddressIndexUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<GetAddressIndexUseCase.Params, Int>(ioDispatcher) {

    override suspend fun execute(parameters: Params): Int {
        return nunchukNativeSdk.getAddressIndex(parameters.walletId, parameters.address)
    }

    class Params(val walletId: String, val address: String)
}
