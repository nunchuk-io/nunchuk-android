package com.nunchuk.android.core.domain

import android.nfc.NdefRecord
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class HealthCheckColdCardUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<HealthCheckColdCardUseCase.Param, HealthStatus?>(dispatcher) {

    override suspend fun execute(parameters: Param): HealthStatus? {
        return nunchukNativeSdk.healthCheckColdCard(parameters.signer, parameters.records.toTypedArray())
    }

    class Param(val signer: SingleSigner, val records: List<NdefRecord>)
}