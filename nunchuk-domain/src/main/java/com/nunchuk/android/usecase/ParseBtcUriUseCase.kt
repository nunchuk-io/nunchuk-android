package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseBtcUriUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<String, BtcUri>(dispatcher) {
    override suspend fun execute(parameters: String): BtcUri {
        return nunchukNativeSdk.parseBtcUri(parameters)
    }
}