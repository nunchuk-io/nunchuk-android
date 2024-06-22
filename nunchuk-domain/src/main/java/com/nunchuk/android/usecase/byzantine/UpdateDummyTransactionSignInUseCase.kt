package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DummyTransactionUpdate
import com.nunchuk.android.model.byzantine.SignInDummyTransactionUpdate
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateDummyTransactionSignInUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dummyTransactionRepository: DummyTransactionRepository,
) : UseCase<UpdateDummyTransactionSignInUseCase.Param, SignInDummyTransactionUpdate>(ioDispatcher) {

    override suspend fun execute(parameters: Param): SignInDummyTransactionUpdate =
        dummyTransactionRepository.updateDummyTransactionSignIn(
            signatures = parameters.signatures,
            dummyTransactionId = parameters.transactionId
        )

    data class Param(
        val transactionId: String,
        val signatures: Map<String, String>
    )
}