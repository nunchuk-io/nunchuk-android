package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CancelFreeGroupDummyTransactionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<CancelFreeGroupDummyTransactionUseCase.Param, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Param) =
        dummyTransactionRepository.cancelFreeGroupDummyTransaction(
            walletId = parameters.walletId,
            dummyTransactionId = parameters.dummyTransactionId
        )

    data class Param(
        val walletId: String,
        val dummyTransactionId: String,
    )
}
