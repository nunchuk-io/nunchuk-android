package com.nunchuk.android.core.domain

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.HealthStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface HealthCheckMasterSignerUseCase {
    fun execute(
        fingerprint: String,
        message: String,
        signature: String,
        path: String,
        masterSignerId: String?
    ): Flow<HealthStatus>
}

internal class HealthCheckMasterSignerUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : HealthCheckMasterSignerUseCase {

    override fun execute(
        fingerprint: String,
        message: String,
        signature: String,
        path: String,
        masterSignerId: String?
    ) = flow {
        emit(
            nunchukNativeSdk.healthCheckMasterSigner(fingerprint, message, signature, path)
        )
        if (masterSignerId.isNullOrEmpty().not()) nunchukNativeSdk.clearSignerPassphrase(masterSignerId.orEmpty())
    }

}