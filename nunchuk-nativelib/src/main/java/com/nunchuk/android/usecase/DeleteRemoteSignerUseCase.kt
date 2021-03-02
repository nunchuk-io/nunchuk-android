package com.nunchuk.android.usecase

interface DeleteRemoteSignerUseCase {
    fun execute(masterFingerprint: String, derivationPath: String): Boolean
}