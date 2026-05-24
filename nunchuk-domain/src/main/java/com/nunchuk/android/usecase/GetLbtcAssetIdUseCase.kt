package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetLbtcAssetIdUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<Unit, String>(ioDispatcher) {
    override suspend fun execute(parameters: Unit): String = nativeSdk.getLBTCAssetId()
}
