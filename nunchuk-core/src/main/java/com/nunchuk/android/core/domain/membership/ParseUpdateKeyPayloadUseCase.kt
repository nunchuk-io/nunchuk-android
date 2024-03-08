package com.nunchuk.android.core.domain.membership

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.byzantine.UpdateGroupKeyPayload
import com.nunchuk.android.core.data.model.membership.toKeyPolicy
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseUpdateKeyPayloadUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val gson: Gson,
) : UseCase<DummyTransactionPayload, KeyPolicy>(dispatcher) {
    override suspend fun execute(parameters: DummyTransactionPayload): KeyPolicy {
        if (parameters.type != DummyTransactionType.UPDATE_SERVER_KEY) throw IllegalArgumentException(
            "Can not parse ${parameters.type}"
        )
        val payload = gson.fromJson(parameters.payload, UpdateGroupKeyPayload::class.java)
        return payload.newPolicies?.toKeyPolicy()
            ?: throw NullPointerException("new policy null")
    }
}