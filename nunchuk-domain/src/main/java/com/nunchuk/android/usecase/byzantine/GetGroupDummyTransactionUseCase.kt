package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetGroupDummyTransactionUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<GetGroupDummyTransactionUseCase.Param, Transaction>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Transaction =
        dummyTransactionRepository.getDummyTransaction(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            dummyTransactionId = parameters.transactionId
        )

    data class Param(val groupId: String, val walletId: String, val transactionId: String)
}