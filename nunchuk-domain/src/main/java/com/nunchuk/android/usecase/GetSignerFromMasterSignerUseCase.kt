package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSignerFromMasterSignerUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetSignerFromMasterSignerUseCase.Params, SingleSigner>(dispatcher) {
    override suspend fun execute(parameters: Params): SingleSigner {
        return nunchukNativeSdk.getSignerFromMasterSigner(
            masterSignerId = parameters.masterSignerId,
            path = parameters.newPath
        )
    }

    data class Params(val masterSignerId: String, val newPath: String)
}