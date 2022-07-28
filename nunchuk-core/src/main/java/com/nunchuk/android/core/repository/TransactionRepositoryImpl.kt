package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.TransactionApi
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.repository.TransactionRepository
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionApi: TransactionApi
) : TransactionRepository {
    override suspend fun getFees(): EstimateFeeRates {
        val data = transactionApi.getFees()
        return EstimateFeeRates(data.priorityRate, data.standardRate, data.economicRate)
    }
}