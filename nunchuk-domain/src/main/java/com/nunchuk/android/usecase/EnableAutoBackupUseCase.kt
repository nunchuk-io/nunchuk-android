package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface EnableAutoBackupUseCase {
    fun execute(
        enable: Boolean
    ): Flow<Boolean>
}

internal class EnableAutoBackupUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : EnableAutoBackupUseCase {

    override fun execute(
        enable: Boolean
    ) = flow {
        try {
            nativeSdk.enableAutoBackUp(enable = enable)
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
        }
        emit(
            enable
        )
    }

}