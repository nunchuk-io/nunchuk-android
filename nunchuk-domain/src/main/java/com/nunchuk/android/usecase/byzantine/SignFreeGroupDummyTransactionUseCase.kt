package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DummyTransactionUpdate
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignFreeGroupDummyTransactionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<SignFreeGroupDummyTransactionUseCase.Param, DummyTransactionUpdate>(ioDispatcher) {

    override suspend fun execute(parameters: Param): DummyTransactionUpdate =
        dummyTransactionRepository.signFreeGroupDummyTransaction(
            walletId = parameters.walletId,
            dummyTransactionId = parameters.dummyTransactionId,
            signatures = parameters.signatures
        )

    data class Param(
        val walletId: String,
        val dummyTransactionId: String,
        val signatures: Map<String, String>,
    )
}
