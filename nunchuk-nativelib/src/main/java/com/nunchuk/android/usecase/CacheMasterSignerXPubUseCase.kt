package com.nunchuk.android.usecase

interface CacheMasterSignerXPubUseCase {
    fun execute(mastersignerId: String, progress: (Boolean, Int) -> Unit)
}