package com.nunchuk.android.usecase

interface DeleteMasterSignerUseCase {
    fun execute(masterSignerId: String): Boolean
}