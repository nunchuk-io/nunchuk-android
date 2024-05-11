package com.nunchuk.android.core.domain.byzantine

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.byzantine.ChangeEmail
import com.nunchuk.android.core.data.model.byzantine.ChangeEmailPayload
import com.nunchuk.android.core.data.model.byzantine.UpdateGroupKeyPayload
import com.nunchuk.android.core.data.model.byzantine.toDomainModel
import com.nunchuk.android.core.data.model.membership.toGroupKeyPolicy
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseChangeEmailPayloadUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val gson: Gson,
) : UseCase<DummyTransactionPayload, ChangeEmail>(dispatcher) {
    override suspend fun execute(parameters: DummyTransactionPayload): ChangeEmail {
        if (parameters.type != DummyTransactionType.CHANGE_EMAIL) throw IllegalArgumentException(
            "Can not parse ${parameters.type}"
        )
        val payload = gson.fromJson(parameters.payload, ChangeEmailPayload::class.java)
        return payload.toDomainModel()
    }
}