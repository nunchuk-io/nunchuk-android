package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner

interface CreateCoboSignerUseCase {
    fun execute(name: String, jsonInfo: String): SingleSigner
}