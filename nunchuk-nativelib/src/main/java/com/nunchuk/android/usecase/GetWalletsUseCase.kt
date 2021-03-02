package com.nunchuk.android.usecase

import com.nunchuk.android.model.*

interface GetWalletsUseCase {
    fun execute(): List<Wallet>
}

