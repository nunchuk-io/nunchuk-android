package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.TxInput

interface GetTotalAmountUseCase {
    fun execute(inputs: List<TxInput>): Amount
}