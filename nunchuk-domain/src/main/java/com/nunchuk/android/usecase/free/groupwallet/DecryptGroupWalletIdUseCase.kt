package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DecryptGroupWalletIdUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<DecryptGroupWalletIdUseCase.Param, String>(ioDispatcher) {
    override suspend fun execute(parameters: Param): String {
        return nativeSdk.decryptGroupWalletId(
            walletId = parameters.walletId,
        )
    }

    data class Param(val walletId: String)
}