package com.nunchuk.android.usecase

import com.nunchuk.android.type.HealthStatus

interface GetHealthCheckMasterSignerUseCase {
    fun execute(fingerprint: String, message: String, signature: String, path: String): HealthStatus
}