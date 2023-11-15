package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetDummyTxRequestTokenUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<GetDummyTxRequestTokenUseCase.Param, Map<String, Boolean>>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Map<String, Boolean> =
        dummyTransactionRepository.getDummyTxRequestToken(
            walletId = parameters.walletId,
            dummyTransactionId = parameters.transactionId
        )

    data class Param(
        val walletId: String,
        val transactionId: String,
    )
}