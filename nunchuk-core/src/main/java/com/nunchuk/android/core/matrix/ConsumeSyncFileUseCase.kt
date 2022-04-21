package com.nunchuk.android.core.matrix

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/*
* Android Matrix SDK does not have a native upload func, so we have to call rest api
* */
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
        //nunchukNativeSdk.consumeSyncFile(fileJsonInfo, fileData)
    }
}
