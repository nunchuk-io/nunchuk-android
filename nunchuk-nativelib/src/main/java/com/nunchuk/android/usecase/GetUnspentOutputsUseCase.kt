package com.nunchuk.android.usecase

import com.nunchuk.android.model.UnspentOutput

interface GetUnspentOutputsUseCase {
    fun execute(walletId: String): List<UnspentOutput>
}