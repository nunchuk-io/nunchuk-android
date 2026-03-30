package com.nunchuk.android.repository

import com.nunchuk.android.model.wallet.GroupInvitation
import com.nunchuk.android.model.wallet.Invitation

interface SharedWalletRepository {
    suspend fun createInvitation(groupId: String, emails: List<String>): List<Invitation>
    suspend fun getGroupInvitations(groupId: String): List<GroupInvitation>
    suspend fun removeInvitation(invitationId: String)
    suspend fun getInvitations(): List<Invitation>
    suspend fun denyInvitation(invitationId: String)
}
