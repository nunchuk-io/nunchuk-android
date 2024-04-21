package com.nunchuk.android.auth.domain

import com.nunchuk.android.core.profile.GetOnBoardUseCase
import com.nunchuk.android.core.profile.SetOnBoardUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class CheckShowOnboardUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val getOnBoardUseCase: GetOnBoardUseCase,
    private val setOnBoardUseCase: SetOnBoardUseCase,
    private val getUserSubscriptionUseCase: GetUserSubscriptionUseCase,
    private val syncGroupWalletsUseCase: SyncGroupWalletsUseCase,
) : UseCase<Unit, Unit>(dispatcher) {
    override suspend fun execute(parameters: Unit) {
        val shouldShowOnboard = getOnBoardUseCase(Unit).first().getOrElse { null }
        if (shouldShowOnboard != false) {
            supervisorScope {
                val subscription = async {
                    getUserSubscriptionUseCase(Unit).map { it.plans }
                        .getOrElse { emptyList() }
                }

                val groupWallets = async {
                    syncGroupWalletsUseCase(Unit).getOrElse { false }
                }

                if (subscription.await().isEmpty() && !groupWallets.await()) {
                    setOnBoardUseCase(true)
                }
            }
        }
    }
}