package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount

interface GetAddressBalanceUseCase {
    fun execute(walletId: String, address: String): Amount
}