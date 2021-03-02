package com.nunchuk.android.usecase

interface GetAddressesUseCase {
    fun execute(walletId: String, used: Boolean = false, internal: Boolean = false): List<String>
}