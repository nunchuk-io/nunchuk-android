package com.nunchuk.android.usecase

interface CacheMasterSignerXPubUseCase {
    fun execute(masterSignerId: String, progress: (Boolean, Int) -> Unit)
}