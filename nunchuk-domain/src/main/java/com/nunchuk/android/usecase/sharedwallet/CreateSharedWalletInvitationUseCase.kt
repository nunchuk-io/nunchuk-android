package com.nunchuk.android.usecase.sharedwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.wallet.Invitation
import com.nunchuk.android.repository.SharedWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateSharedWalletInvitationUseCase @Inject constructor(
    private val repository: SharedWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<CreateSharedWalletInvitationUseCase.Param, List<Invitation>>(dispatcher) {

    override suspend fun execute(parameters: Param): List<Invitation> {
        return repository.createInvitation(
            groupId = parameters.groupId,
            emails = parameters.emails,
        )
    }

    data class Param(val groupId: String, val emails: List<String>)
}
