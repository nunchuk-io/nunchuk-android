package com.nunchuk.android.usecase.premier

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.repository.RecurringPaymentRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateRecurringPaymentUseCase @Inject constructor(
    private val repository: RecurringPaymentRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<CreateRecurringPaymentUseCase.Params, String>(dispatcher) {
    override suspend fun execute(parameters: Params): String {
        return repository.createRecurringPayment(
            parameters.groupId,
            parameters.walletId,
            parameters.recurringPayment,
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
        val recurringPayment: RecurringPayment,
    )
}