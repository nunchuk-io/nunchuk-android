package com.nunchuk.android.core.domain.byzantine

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.byzantine.PendingHealthCheckPayload
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.DummyTransaction
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.PendingHealthCheck
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParsePendingHealthCheckPayloadUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val gson: Gson,
) : UseCase<DummyTransaction, PendingHealthCheck>(dispatcher) {
    override suspend fun execute(parameters: DummyTransaction): PendingHealthCheck {
        if (parameters.dummyTransactionType != DummyTransactionType.HEALTH_CHECK_PENDING && parameters.dummyTransactionType != DummyTransactionType.HEALTH_CHECK_REQUEST) throw IllegalArgumentException(
            "Can not parse ${parameters.dummyTransactionType}"
        )
        val payload = gson.fromJson(parameters.payload, PendingHealthCheckPayload::class.java)
        return PendingHealthCheck(
            keyXfp = payload.keyXfp,
            groupId = payload.groupId,
            walletId = payload.walletId,
            walletLocalId = payload.walletLocalId,
        )
    }
}