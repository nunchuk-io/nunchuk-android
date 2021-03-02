package com.nunchuk.android.usecase

import com.nunchuk.android.model.Device

interface GetDevicesUseCase {
    fun execute(): List<Device>
}