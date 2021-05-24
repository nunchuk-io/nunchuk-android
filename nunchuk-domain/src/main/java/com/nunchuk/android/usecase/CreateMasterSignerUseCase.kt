package com.nunchuk.android.usecase

import com.nunchuk.android.model.Device
import com.nunchuk.android.model.MasterSigner

interface CreateMasterSignerUseCase {
    fun execute(name: String, device: Device, progress: (Boolean, Int) -> Unit): MasterSigner
}