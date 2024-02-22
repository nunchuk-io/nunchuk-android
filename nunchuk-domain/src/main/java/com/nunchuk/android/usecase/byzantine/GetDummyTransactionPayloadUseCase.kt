package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetDummyTransactionPayloadUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<GetDummyTransactionPayloadUseCase.Param, DummyTransactionPayload>(ioDispatcher) {

    override suspend fun execute(parameters: Param): DummyTransactionPayload =
        dummyTransactionRepository.getDummyTransactionPayload(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            dummyTransactionId = parameters.transactionId
        )

    data class Param(val groupId: String = "", val walletId: String, val transactionId: String)
}