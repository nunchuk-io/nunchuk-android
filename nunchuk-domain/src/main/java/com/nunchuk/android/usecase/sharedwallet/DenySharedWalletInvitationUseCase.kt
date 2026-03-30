package com.nunchuk.android.usecase.sharedwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SharedWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DenySharedWalletInvitationUseCase @Inject constructor(
    private val repository: SharedWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<DenySharedWalletInvitationUseCase.Param, Unit>(dispatcher) {

    override suspend fun execute(parameters: Param) {
        return repository.denyInvitation(invitationId = parameters.invitationId)
    }

    data class Param(val invitationId: String)
}
