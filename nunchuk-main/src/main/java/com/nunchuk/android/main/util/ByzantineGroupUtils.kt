package com.nunchuk.android.main.util

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.ByzantineGroupBrief
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import javax.inject.Inject

class ByzantineGroupUtils @Inject constructor(private val accountManager: AccountManager) {

    fun getCurrentUserRole(group: ByzantineGroupBrief?): String {
        if (group == null) return AssistedWalletRole.NONE.name
        return group.members.firstOrNull {
            isMatchingEmailOrUserName(it.emailOrUsername)
        }?.role ?: AssistedWalletRole.NONE.name
    }

    private fun isMatchingEmailOrUserName(emailOrUsername: String) =
        emailOrUsername == accountManager.getAccount().email
                || emailOrUsername == accountManager.getAccount().username

    fun getInviterName(group: ByzantineGroupBrief): String {
        val invitee =
            group.members.firstOrNull {
                it.isPendingRequest() && isMatchingEmailOrUserName(it.emailOrUsername)
            } ?: return ""
        val inviter = group.members.firstOrNull { it.userId == invitee.inviterUserId }
        return inviter?.name.orEmpty()
    }
}