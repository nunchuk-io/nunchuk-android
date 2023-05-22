package com.nunchuk.android.main.membership.byzantine.initvitemember

import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.WalletConstraints
import com.nunchuk.android.model.byzantine.AssistedMember

sealed class ByzantineInviteMembersEvent {
    data class Loading(val loading: Boolean) : ByzantineInviteMembersEvent()
    data class Error(val message: String) : ByzantineInviteMembersEvent()
    data class CreateGroupWalletSuccess(val groupId: String) : ByzantineInviteMembersEvent()
    object PrimaryKeyAdminRoleWarning : ByzantineInviteMembersEvent()
    object LimitKeyholderRoleWarning : ByzantineInviteMembersEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val userData: String,
        val requiredSignatures: Int
    ) : ByzantineInviteMembersEvent()

    data class EditGroupMemberSuccess(val members: List<ByzantineMember>) : ByzantineInviteMembersEvent()
}

data class ByzantineInviteMembersState(
    val members: List<AssistedMember> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val suggestionContacts: List<Contact> = emptyList(),
    val selectContacts: HashSet<String> = hashSetOf(),
    val preMembers: List<AssistedMember> = emptyList(),
    val walletConstraints: WalletConstraints? = null,
    val interactingIndex: Int = -1
)