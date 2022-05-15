package com.nunchuk.android.core.matrix

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface RegisterDownloadBackUpFileUseCase {
    fun execute(): Flow<Unit>
}

internal class RegisterDownloadBackUpFileUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : RegisterDownloadBackUpFileUseCase {

    override fun execute() = flow {
        emit(nunchukNativeSdk.registerDownloadFileBackup())
    }.flowOn(Dispatchers.IO)
}
