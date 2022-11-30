package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class VerifyTapSignerBackupContentUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<VerifyTapSignerBackupContentUseCase.Param, Boolean>(dispatcher) {
    override suspend fun execute(parameters: Param): Boolean {
        return nunchukNativeSdk.verifyTapSignerBackupContent(
            masterSignerId = parameters.masterSignerId,
            backUpKey = parameters.backUpKey,
            content = parameters.content
        )
    }

    class Param(val masterSignerId: String, val backUpKey: String, val content: ByteArray)
}