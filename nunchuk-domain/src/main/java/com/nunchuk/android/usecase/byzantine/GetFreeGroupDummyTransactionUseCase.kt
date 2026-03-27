package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.DummyTransaction
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetFreeGroupDummyTransactionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<GetFreeGroupDummyTransactionUseCase.Param, DummyTransaction>(ioDispatcher) {

    override suspend fun execute(parameters: Param): DummyTransaction =
        dummyTransactionRepository.getFreeGroupDummyTransaction(
            walletId = parameters.walletId,
            dummyTransactionId = parameters.dummyTransactionId
        )

    data class Param(
        val walletId: String,
        val dummyTransactionId: String,
    )
}
