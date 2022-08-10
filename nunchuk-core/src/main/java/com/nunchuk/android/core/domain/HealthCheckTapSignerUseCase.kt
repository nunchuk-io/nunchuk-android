package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.HealthStatus
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class HealthCheckTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<HealthCheckTapSignerUseCase.Data, HealthStatus>(dispatcher, waitAutoCardUseCase) {

    override suspend fun executeNfc(parameters: Data): HealthStatus {
        return nunchukNativeSdk.healthCheckTapSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            fingerprint = parameters.fingerprint,
            message = parameters.message,
            signature = parameters.signature,
            path = parameters.path
        )
    }

    class Data(
        isoDep: IsoDep,
        val cvc: String,
        val fingerprint: String,
        val message: String,
        val signature: String,
        val path: String
    ) : BaseNfcUseCase.Data(isoDep)
}