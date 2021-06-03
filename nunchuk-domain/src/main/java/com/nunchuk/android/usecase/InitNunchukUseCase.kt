package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface InitNunchukUseCase {
    suspend fun execute(appSettings: AppSettings): Result<Unit>
}

internal class InitNunchukUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), InitNunchukUseCase {

    override suspend fun execute(appSettings: AppSettings) = exe {
        nativeSdk.initNunchuk(appSettings)
    }

}