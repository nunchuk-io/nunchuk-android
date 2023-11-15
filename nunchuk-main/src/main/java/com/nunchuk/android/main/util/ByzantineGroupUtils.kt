package com.nunchuk.android.main.util

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import javax.inject.Inject

class ByzantineGroupUtils @Inject constructor(private val accountManager: AccountManager) {

    private val accountInfo: AccountInfo by lazy { accountManager.getAccount() }

    fun getCurrentUserRole(group: ByzantineGroup?): String {
        if (group == null) return AssistedWalletRole.NONE.name
        return group.members.firstOrNull {
            isMatchingEmailOrUserName(it.emailOrUsername)
        }?.role ?: AssistedWalletRole.NONE.name
    }

    private fun isMatchingEmailOrUserName(emailOrUsername: String) =
        emailOrUsername == accountInfo.email
                || emailOrUsername == accountInfo.username

    fun getInviterName(group: ByzantineGroup): String {
        val invitee =
            group.members.firstOrNull {
                it.isPendingRequest() && isMatchingEmailOrUserName(it.emailOrUsername)
            } ?: return ""
        val inviter = group.members.firstOrNull { it.user?.id == invitee.inviterUserId }
        return inviter?.user?.name.orEmpty()
    }

    fun isPendingAcceptInvite(group: ByzantineGroup): Boolean {
        return group.members.any { it.isPendingRequest() && isMatchingEmailOrUserName(it.emailOrUsername) }
    }
}