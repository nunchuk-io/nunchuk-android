package com.nunchuk.android.core.matrix

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ConsumeSyncFileUseCase {
    fun execute(
        fileJsonInfo: String,
        fileData: ByteArray
    ): Flow<Unit>
}
internal class ConsumeSyncFileUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : ConsumeSyncFileUseCase {

    override fun execute(
        fileJsonInfo: String,
        fileData: ByteArray
    ) = flow<Unit> {
        nunchukNativeSdk.downloadFileCallback(fileJsonInfo = fileJsonInfo, fileData = fileData)
    }
}
