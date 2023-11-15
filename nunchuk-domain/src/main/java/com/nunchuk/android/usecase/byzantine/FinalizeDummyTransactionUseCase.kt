package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class FinalizeDummyTransactionUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<FinalizeDummyTransactionUseCase.Params, Unit>(dispatcher) {
    override suspend fun execute(parameters: Params) {
        return dummyTransactionRepository.finalizeDummyTransaction(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            dummyTransactionId = parameters.dummyTransactionId,
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
        val dummyTransactionId: String,
    )
}