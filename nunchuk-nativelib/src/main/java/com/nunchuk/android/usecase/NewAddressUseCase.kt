package com.nunchuk.android.usecase

interface NewAddressUseCase {
    fun execute(wallet_id: String, internal: Boolean = false): String
}