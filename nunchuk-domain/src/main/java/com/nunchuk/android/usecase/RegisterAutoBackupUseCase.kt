package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface RegisterAutoBackupUseCase {
    fun execute(
        syncRoomId: String,
        accessToken: String
    ): Flow<Unit>
}

internal class RegisterAutoBackupUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : RegisterAutoBackupUseCase {

    override fun execute(
        syncRoomId: String,
        accessToken: String
    ) = flow {
        emit(
            try {
                nativeSdk.registerAutoBackUp(syncRoomId = syncRoomId, accessToken = accessToken)
            } catch (t: Throwable) {
                CrashlyticsReporter.recordException(t)
            }
        )
    }.flowOn(Dispatchers.IO)

}