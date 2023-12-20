package com.nunchuk.android.usecase.premier

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.repository.RecurringPaymentRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetRecurringPaymentUseCase @Inject constructor(
    private val repository: RecurringPaymentRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<GetRecurringPaymentUseCase.Params, RecurringPayment>(dispatcher) {
    override suspend fun execute(parameters: Params): RecurringPayment {
        return repository.getRecurringPayment(
            parameters.groupId,
            parameters.walletId,
            parameters.id,
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
        val id: String
    )
}