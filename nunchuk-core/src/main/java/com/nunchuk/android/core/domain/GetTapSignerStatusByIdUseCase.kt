package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTapSignerStatusByIdUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<String, TapSignerStatus>(dispatcher) {
    override suspend fun execute(parameters: String): TapSignerStatus {
        return nunchukNativeSdk.getTapSignerStatusFromMasterSigner(parameters)
    }
}