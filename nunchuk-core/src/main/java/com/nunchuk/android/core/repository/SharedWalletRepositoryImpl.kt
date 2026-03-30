package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.model.sharedwallet.CreateInvitationRequest
import com.nunchuk.android.core.data.model.sharedwallet.GroupInvitationDto
import com.nunchuk.android.core.data.model.sharedwallet.InvitationDto
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.model.wallet.GroupInvitation
import com.nunchuk.android.model.wallet.Invitation
import com.nunchuk.android.repository.SharedWalletRepository
import javax.inject.Inject

internal class SharedWalletRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
) : SharedWalletRepository {

    override suspend fun createInvitation(
        groupId: String,
        emails: List<String>,
    ): List<Invitation> {
        return userWalletApiManager.sharedWalletApi.createInvitation(
            CreateInvitationRequest(groupId = groupId, emails = emails)
        ).data.invitations.orEmpty().map { it.toDomainModel() }
    }

    override suspend fun getGroupInvitations(groupId: String): List<GroupInvitation> {
        return userWalletApiManager.sharedWalletApi.getGroupInvitations(groupId)
            .data.invitations.orEmpty().map { it.toDomainModel() }
    }

    override suspend fun removeInvitation(invitationId: String) {
        val response = userWalletApiManager.sharedWalletApi.removeInvitation(invitationId)
        if (response.isSuccess.not()) throw response.error
    }

    override suspend fun getInvitations(): List<Invitation> {
        return userWalletApiManager.sharedWalletApi.getInvitations()
            .data.invitations.orEmpty().map { it.toDomainModel() }
    }

    override suspend fun denyInvitation(invitationId: String) {
        val response = userWalletApiManager.sharedWalletApi.denyInvitation(invitationId)
        if (response.isSuccess.not()) throw response.error
    }
}

private fun InvitationDto.toDomainModel() = Invitation(
    id = id.orEmpty(),
    inviterName = inviterName.orEmpty(),
    inviterEmail = inviterEmail.orEmpty(),
    groupId = groupId.orEmpty(),
)

private fun GroupInvitationDto.toDomainModel() = GroupInvitation(
    id = id.orEmpty(),
    groupId = groupId.orEmpty(),
    recipientEmail = recipientEmail.orEmpty(),
    recipientUserId = recipientUserId.orEmpty(),
    status = status.orEmpty(),
    createdTime = createdTime,
)
