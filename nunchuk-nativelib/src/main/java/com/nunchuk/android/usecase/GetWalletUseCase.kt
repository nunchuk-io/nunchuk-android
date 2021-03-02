package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet

interface GetWalletUseCase {
    fun execute(walletId: String): Wallet
}