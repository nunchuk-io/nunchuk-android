package com.nunchuk.android.core.matrix

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.trySafe
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
        emit(trySafe(nunchukNativeSdk::registerDownloadFileBackup) ?: Unit)
    }.flowOn(Dispatchers.IO)
}
