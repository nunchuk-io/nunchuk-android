package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner

interface UpdateRemoteSignerUseCase {
    fun execute(remotesigner: SingleSigner): Boolean
}