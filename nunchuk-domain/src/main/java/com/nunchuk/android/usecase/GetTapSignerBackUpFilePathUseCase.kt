package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTapSignerBackUpFilePathUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<String, TapSignerStatus>(dispatcher) {
    override suspend fun execute(parameters: String): TapSignerStatus {
        return nunchukNativeSdk.getTapSignerStatusFromMasterSigner(parameters)
    }
}