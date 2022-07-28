package com.nunchuk.android.repository

import com.nunchuk.android.model.EstimateFeeRates

interface TransactionRepository {
    suspend fun getFees(): EstimateFeeRates
}