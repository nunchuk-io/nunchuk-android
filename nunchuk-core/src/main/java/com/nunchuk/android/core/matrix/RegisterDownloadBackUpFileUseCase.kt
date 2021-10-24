package com.nunchuk.android.core.matrix

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface RegisterDownloadBackUpFileUseCase {
    fun execute(
    ): Flow<Unit>
}

internal class RegisterDownloadBackUpFileUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : RegisterDownloadBackUpFileUseCase {

    override fun execute() = flow<Unit> {
        nunchukNativeSdk.registerDownloadFileBackup()
    }
}
