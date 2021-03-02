package com.nunchuk.android.usecase

interface ExportCoboWalletUseCase {
    fun execute(walletId: String): List<String>
}