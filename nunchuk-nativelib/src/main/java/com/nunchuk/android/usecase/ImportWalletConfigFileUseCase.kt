package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet

interface ImportWalletConfigFileUseCase {
    fun execute(filePath: String, description: String = ""): Wallet
}