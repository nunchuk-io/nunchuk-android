package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet

interface ImportCoboWalletUseCase {
    fun execute(qrData: List<String>, description: String): Wallet
}