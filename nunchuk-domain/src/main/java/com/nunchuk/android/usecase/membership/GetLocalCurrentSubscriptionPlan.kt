package com.nunchuk.android.usecase.membership

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.repository.MembershipRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLocalCurrentSubscriptionPlan @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: MembershipRepository
) : FlowUseCase<Unit, MembershipPlan>(dispatcher) {
    override fun execute(parameters: Unit): Flow<MembershipPlan> {
        return repository.getLocalCurrentPlan()
    }
}