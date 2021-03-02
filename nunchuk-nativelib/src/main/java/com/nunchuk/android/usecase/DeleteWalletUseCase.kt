package com.nunchuk.android.usecase

interface DeleteWalletUseCase {
    fun execute(walletId: String): Boolean
}