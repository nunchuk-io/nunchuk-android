package com.nunchuk.android.core.domain.byzantine

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.byzantine.InheritancePayload
import com.nunchuk.android.core.data.model.byzantine.InheritancePayloadDto
import com.nunchuk.android.core.data.model.byzantine.toInheritancePayload
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.isInheritanceFlow
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseInheritancePayloadUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val gson: Gson,
) : UseCase<DummyTransactionPayload, InheritancePayload>(dispatcher) {
    override suspend fun execute(parameters: DummyTransactionPayload): InheritancePayload {
        if (parameters.type.isInheritanceFlow().not()) throw IllegalArgumentException(
            "Can not parse ${parameters.type}"
        )
        return gson.fromJson(parameters.payload, InheritancePayloadDto::class.java)
            .toInheritancePayload()
    }
}