package com.nunchuk.android.usecase.sharedwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.wallet.GroupInvitation
import com.nunchuk.android.repository.SharedWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetGroupInvitationsUseCase @Inject constructor(
    private val repository: SharedWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<GetGroupInvitationsUseCase.Param, List<GroupInvitation>>(dispatcher) {

    override suspend fun execute(parameters: Param): List<GroupInvitation> {
        return repository.getGroupInvitations(groupId = parameters.groupId)
    }

    data class Param(val groupId: String)
}
