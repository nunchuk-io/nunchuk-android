package com.nunchuk.android.usecase

interface DisplayAddressOnDeviceUseCase {
    fun execute(walletId: String, address: String, deviceFingerprint: String)
}