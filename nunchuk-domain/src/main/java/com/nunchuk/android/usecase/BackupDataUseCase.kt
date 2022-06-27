package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface BackupDataUseCase {
    fun execute(
    ): Flow<Unit>
}

internal class BackupDataUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BackupDataUseCase {

    override fun execute(
    ) = flow {
        emit(
            try {
                nativeSdk.backup()
            } catch (t: Throwable) {
                CrashlyticsReporter.recordException(t)
            }
        )
    }

}