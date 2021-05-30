package com.nunchuk.android.usecase

import com.nunchuk.android.model.Device
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetDevicesUseCase {
    suspend fun execute(): Result<List<Device>>
}

internal class GetDevicesUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetDevicesUseCase {

    override suspend fun execute() = exe {
        nunchukFacade.getDevices()
    }

}