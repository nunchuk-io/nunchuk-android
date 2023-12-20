package com.nunchuk.android.core.domain.byzantine

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.byzantine.RecurringPaymentPayload
import com.nunchuk.android.core.data.model.payment.toModel
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ParseRecurringPaymentPayloadUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val gson: Gson,
) : UseCase<DummyTransactionPayload, RecurringPayment>(dispatcher) {
    override suspend fun execute(parameters: DummyTransactionPayload): RecurringPayment {
        if (parameters.type != DummyTransactionType.CREATE_RECURRING_PAYMENT) throw IllegalArgumentException(
            "Can not parse ${parameters.type}"
        )
        val payload = gson.fromJson(parameters.payload, RecurringPaymentPayload::class.java)
        return payload.data?.toModel()
            ?: throw NullPointerException("new policy null")
    }
}