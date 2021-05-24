package com.nunchuk.android.usecase

interface GetNumberOfSignersFromMasterSignerUseCase {
    fun execute(mastersignerId: String): Int
}