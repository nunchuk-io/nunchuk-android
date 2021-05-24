package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet

interface ImportWalletDbUseCase {
    fun execute(filePath: String): Wallet
}