package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class KeyHealthCheckUseCase @Inject constructor(
    private val repository: GroupWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<KeyHealthCheckUseCase.Params, DummyTransactionPayload>(ioDispatcher) {

    override suspend fun execute(parameters: Params) : DummyTransactionPayload{
        return repository.healthCheck(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            xfp = parameters.xfp
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
        val xfp: String,
    )
}