package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount

interface EstimateFeeUseCase {
    fun execute(confTarget: Int = 6): Amount
}