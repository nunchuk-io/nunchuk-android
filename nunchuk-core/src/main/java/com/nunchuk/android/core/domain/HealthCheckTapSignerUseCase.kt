package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import javax.inject.Inject

class HealthCheckTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<HealthCheckTapSignerUseCase.Data, HealthStatus>(dispatcher) {
    override suspend fun execute(parameters: Data): HealthStatus {
        val card = parameters.isoDep
        card.timeout = NFC_CARD_TIMEOUT
        card.connect()
        try {
            if (card.isConnected) {
                return nunchukNativeSdk.healthCheckTapSigner(
                    isoDep = parameters.isoDep,
                    cvc = parameters.cvc,
                    fingerprint = parameters.fingerprint,
                    message = parameters.message,
                    signature = parameters.signature,
                    path = parameters.path
                )
            }
        } finally {
            runCatching { card.close() }
        }
        throw IOException("Can not connect nfc card")
    }

    data class Data(val isoDep: IsoDep, val cvc: String, val fingerprint: String, val message: String, val signature: String, val path: String)
}