package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RestartWizardUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: MembershipRepository
) : UseCase<MembershipPlan, Unit>(dispatcher) {
    override suspend fun execute(parameters: MembershipPlan) {
        return repository.restart(parameters)
    }
}