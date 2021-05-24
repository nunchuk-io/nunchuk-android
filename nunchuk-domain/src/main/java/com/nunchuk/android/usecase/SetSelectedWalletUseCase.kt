package com.nunchuk.android.usecase

interface SetSelectedWalletUseCase {
    fun execute(walletId: String): Boolean
}