package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MemberSubscription
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetUserSubscriptionUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: MembershipRepository
) : UseCase<Unit, MemberSubscription>(dispatcher) {
    override suspend fun execute(parameters: Unit) : MemberSubscription {
        return repository.getSubscription()
    }
}