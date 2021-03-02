package com.nunchuk.android.usecase

import com.nunchuk.android.model.Device

interface PromtPinOnDeviceUseCase {
    fun execute(device: Device)
}