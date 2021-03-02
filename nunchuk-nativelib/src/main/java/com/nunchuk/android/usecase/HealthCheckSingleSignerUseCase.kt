package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.HealthStatus

interface HealthCheckSingleSignerUseCase {
    fun execute(signer: SingleSigner, message: String, signature: String): HealthStatus
}