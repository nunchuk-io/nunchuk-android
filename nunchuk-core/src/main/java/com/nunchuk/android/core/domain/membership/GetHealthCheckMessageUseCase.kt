package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetHealthCheckMessageUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetHealthCheckMessageUseCase.Param, String>(dispatcher) {
    override suspend fun execute(parameters: Param): String {
        return nunchukNativeSdk.getHealthCheckMessage(parameters.userData)
    }

    class Param(val userData: String)
}