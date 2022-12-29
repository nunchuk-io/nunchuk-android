package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLocalMembershipPlanFlowUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val ncDataStore: NcDataStore
) : FlowUseCase<Unit, MembershipPlan>(dispatcher) {
    override fun execute(parameters: Unit): Flow<MembershipPlan> = ncDataStore.membershipPlan
}