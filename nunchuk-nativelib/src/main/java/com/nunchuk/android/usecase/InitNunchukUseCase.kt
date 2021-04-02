package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface InitNunchukUseCase {
    suspend fun execute(appSettings: AppSettings): Result<Unit>
}

internal class InitNunchukUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), InitNunchukUseCase {

    override suspend fun execute(appSettings: AppSettings) = exe {
        nunchukFacade.initNunchuk(appSettings)
    }

}