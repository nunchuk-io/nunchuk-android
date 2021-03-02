package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner

interface GetMasterSignersUseCase {
    fun execute(): List<MasterSigner>
}