package com.nunchuk.android.main.membership.byzantine.initvitemember

import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.WalletConstraints
import com.nunchuk.android.model.byzantine.AssistedMember

sealed class ByzantineInviteMembersEvent {
    data class Loading(val loading: Boolean) : ByzantineInviteMembersEvent()
    data class Error(val message: String) : ByzantineInviteMembersEvent()
    data class CreateGroupWalletSuccess(val groupId: String) : ByzantineInviteMembersEvent()
    object LimitKeyholderRoleWarning : ByzantineInviteMembersEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val userData: String,
        val requiredSignatures: Int
    ) : ByzantineInviteMembersEvent()

    data class EditGroupMemberSuccess(val members: List<ByzantineMember>) :
        ByzantineInviteMembersEvent()
}

data class ByzantineInviteMembersState(
    val members: List<InviteMemberUi> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val suggestionContacts: List<Contact> = emptyList(),
    val walletConstraints: WalletConstraints? = null,
    val interactingIndex: Int = -1
)

data class InviteMemberUi(
    val role: String,
    val name: String?,
    val email: String,
    val isContact: Boolean = false,
    val err: String? = null,
    val isNewAdded: Boolean = false
)

internal fun InviteMemberUi.toAssistedMember(): AssistedMember {
    return AssistedMember(
        role = role,
        name = name,
        email = email
    )
}

internal fun ByzantineMember.toInviteMemberUi(): InviteMemberUi {
    return InviteMemberUi(
        email = user?.email ?: emailOrUsername,
        role = role,
        name = user?.name.orEmpty(),
        isContact = isContact()
    )
}