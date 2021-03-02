package com.nunchuk.android.usecase

interface DeleteMasterSignerUseCase {
    fun execute(mastersignerId: String): Boolean
}