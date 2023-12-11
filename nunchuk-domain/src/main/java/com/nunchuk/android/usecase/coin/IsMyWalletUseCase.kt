package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject


class IsMyWalletUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<IsMyWalletUseCase.Param, Boolean>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Boolean {
        return  parameters.addresses.any {
            runCatching {
                nativeSdk.isMyAddress(parameters.walletId, it)
            }.getOrDefault(false)
        }
    }

    data class Param(val walletId: String, val addresses: List<String>)
}