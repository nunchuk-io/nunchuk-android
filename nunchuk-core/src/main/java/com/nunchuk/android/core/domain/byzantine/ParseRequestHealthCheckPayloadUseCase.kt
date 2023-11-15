package com.nunchuk.android.core.domain.byzantine

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.byzantine.HealthCheckRequestPayload
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.DummyTransaction
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.HealthCheckRequest
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseRequestHealthCheckPayloadUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val gson: Gson,
) : UseCase<DummyTransaction, HealthCheckRequest>(dispatcher) {
    override suspend fun execute(parameters: DummyTransaction): HealthCheckRequest {
        if (parameters.dummyTransactionType != DummyTransactionType.HEALTH_CHECK_REQUEST) throw IllegalArgumentException(
            "Can not parse ${parameters.dummyTransactionType}"
        )
        val payload = gson.fromJson(parameters.payload, HealthCheckRequestPayload::class.java)
        return HealthCheckRequest(
            keyXfp = payload.keyXfp,
            groupId = payload.groupId,
            walletId = payload.walletId,
            walletLocalId = payload.walletLocalId,
        )
    }
}