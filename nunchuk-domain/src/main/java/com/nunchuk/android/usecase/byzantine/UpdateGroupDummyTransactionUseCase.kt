package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateGroupDummyTransactionUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<UpdateGroupDummyTransactionUseCase.Param, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Unit =
        dummyTransactionRepository.updateDummyTransaction(
            signatures = parameters.signatures,
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            dummyTransactionId = parameters.transactionId
        )

    data class Param(
        val groupId: String,
        val walletId: String,
        val transactionId: String,
        val signatures: Map<String, String>
    )
}