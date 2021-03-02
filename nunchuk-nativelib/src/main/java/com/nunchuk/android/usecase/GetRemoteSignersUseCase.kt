package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner

interface GetRemoteSignersUseCase {
    fun execute(): List<SingleSigner>
}