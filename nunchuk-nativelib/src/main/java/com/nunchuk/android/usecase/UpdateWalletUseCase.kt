package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet

interface UpdateWalletUseCase {
    fun execute(wallet: Wallet): Boolean
}