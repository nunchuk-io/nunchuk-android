package com.nunchuk.android.usecase

import com.nunchuk.android.model.Device

interface SendPinToDeviceUseCase {
    fun execute(device: Device, pin: String)
}