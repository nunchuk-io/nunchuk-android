package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SignInDummyTransaction
import com.nunchuk.android.repository.DummyTransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSignInDummyTransactionUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<GetSignInDummyTransactionUseCase.Param, SignInDummyTransaction>(ioDispatcher) {

    override suspend fun execute(parameters: Param): SignInDummyTransaction =
        dummyTransactionRepository.getSignInDummyTransaction(
            data = parameters.data
        )

    data class Param(val data: String)
}