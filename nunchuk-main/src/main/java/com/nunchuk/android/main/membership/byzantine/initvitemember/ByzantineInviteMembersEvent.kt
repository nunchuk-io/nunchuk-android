package com.nunchuk.android.main.membership.byzantine.initvitemember

import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.byzantine.AssistedMember

sealed class ByzantineInviteMembersEvent {
    data class Loading(val loading: Boolean) : ByzantineInviteMembersEvent()
    data class Error(val message: String) : ByzantineInviteMembersEvent()
    data class CreateGroupWalletSuccess(val groupId: String) : ByzantineInviteMembersEvent()
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
    val preMembers: List<InviteMemberUi> = emptyList(),
    val interactingIndex: Int = -1
)

data class InviteMemberUi(
    val role: String,
    val name: String?,
    val email: String,
    val err: String? = null
)

internal fun InviteMemberUi.toAssistedMember(): AssistedMember {
    return AssistedMember(
        role = role,
        name = name,
        email = email
    )
}