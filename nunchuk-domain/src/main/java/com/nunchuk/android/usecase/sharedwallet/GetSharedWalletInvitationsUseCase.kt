package com.nunchuk.android.usecase.sharedwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.wallet.Invitation
import com.nunchuk.android.repository.SharedWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSharedWalletInvitationsUseCase @Inject constructor(
    private val repository: SharedWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<Unit, List<Invitation>>(dispatcher) {

    override suspend fun execute(parameters: Unit): List<Invitation> {
        return repository.getInvitations()
    }
}
