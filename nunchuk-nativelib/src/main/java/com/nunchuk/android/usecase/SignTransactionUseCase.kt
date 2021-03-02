package com.nunchuk.android.usecase

import com.nunchuk.android.model.Device

interface SignTransactionUseCase {
    fun execute(walletId: String, txId: String, device: Device)
}