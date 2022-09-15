package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import javax.inject.Inject

class VerifyTapSignerBackupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<VerifyTapSignerBackupUseCase.Data, Boolean>(dispatcher) {
    override suspend fun execute(parameters: Data): Boolean {
        return nunchukNativeSdk.verifyTapSignerBackup(
            masterSignerId = parameters.masterSignerId,
            backUpKey = parameters.backUpKey,
            decryptionKey = parameters.decryptionKey
        ).also {
            if (it) runCatching { File(parameters.backUpKey).delete() }
        }
    }

    class Data(val masterSignerId: String, val backUpKey: String, val decryptionKey: String)
}