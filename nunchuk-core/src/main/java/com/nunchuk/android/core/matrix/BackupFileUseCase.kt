package com.nunchuk.android.core.matrix

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface BackupFileUseCase {
    fun execute(
        syncRoomId: String,
        fileJsonInfo: String,
        fileUrl: String
    ): Flow<Unit>
}

internal class BackupFileUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : BackupFileUseCase {

    override fun execute(
        syncRoomId: String,
        fileJsonInfo: String,
        fileUrl: String
    ) = flow<Unit> {
        nunchukNativeSdk.backupFile(syncRoomId, fileJsonInfo, fileUrl)
    }
}
