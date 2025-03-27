package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class MarkHotKeyBackedUpUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<String, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: String) {
        val signer = nativeSdk.getMasterSigner(parameters)
        nativeSdk.updateMasterSigner(
            signer.copy(isNeedBackup = false)
        )
    }
}