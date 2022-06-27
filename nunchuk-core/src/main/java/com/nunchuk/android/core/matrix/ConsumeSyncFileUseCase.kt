package com.nunchuk.android.core.matrix

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.trySafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface ConsumeSyncFileUseCase {
    fun execute(
        fileJsonInfo: String,
        filePath: String
    ): Flow<Unit>
}

internal class ConsumeSyncFileUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : ConsumeSyncFileUseCase {

    override fun execute(
        fileJsonInfo: String,
        filePath: String
    ) = flow<Unit> {
        trySafe {
            nunchukNativeSdk.writeFileCallback(fileJsonInfo = fileJsonInfo, fileData = filePath)
        }
    }.flowOn(Dispatchers.IO)
}
