package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class IsLiquidAddressUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<String, Boolean>(ioDispatcher) {
    override suspend fun execute(parameters: String): Boolean =
        nativeSdk.isLiquidAddress(parameters)
}
