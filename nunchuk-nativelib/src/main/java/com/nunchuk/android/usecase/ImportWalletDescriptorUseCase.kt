package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet

interface ImportWalletDescriptorUseCase {
    fun execute(filePath: String, name: String, description: String = ""): Wallet
}