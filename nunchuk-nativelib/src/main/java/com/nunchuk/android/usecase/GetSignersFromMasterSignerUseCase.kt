package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner

interface GetSignersFromMasterSignerUseCase {
    fun execute(mastersignerId: String): List<SingleSigner>
}