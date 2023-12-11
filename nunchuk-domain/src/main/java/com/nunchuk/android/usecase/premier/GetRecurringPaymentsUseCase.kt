package com.nunchuk.android.usecase.premier

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.repository.RecurringPaymentRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetRecurringPaymentsUseCase @Inject constructor(
    private val repository: RecurringPaymentRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<GetRecurringPaymentsUseCase.Params, List<RecurringPayment>>(dispatcher) {
    override suspend fun execute(parameters: Params): List<RecurringPayment> {
        return repository.getRecurringPayments(
            parameters.groupId,
            parameters.walletId,
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
    )
}