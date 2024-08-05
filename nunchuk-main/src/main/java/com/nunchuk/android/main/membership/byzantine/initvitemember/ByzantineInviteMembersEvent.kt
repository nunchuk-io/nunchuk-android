package com.nunchuk.android.main.membership.byzantine.initvitemember

import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.WalletConstraints
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole

sealed class ByzantineInviteMembersEvent {
    data class Loading(val loading: Boolean) : ByzantineInviteMembersEvent()
    data class Error(val message: String) : ByzantineInviteMembersEvent()
    data class CreateGroupWalletSuccess(val groupId: String) : ByzantineInviteMembersEvent()
    data object LimitKeyholderRoleWarning : ByzantineInviteMembersEvent()
    data object RemoveMemberInheritanceWarning : ByzantineInviteMembersEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val userData: String,
        val requiredSignatures: Int
    ) : ByzantineInviteMembersEvent()

    data class EditGroupMemberSuccess(val members: List<ByzantineMember>) :
        ByzantineInviteMembersEvent()
    data class FacilitatorAdminWarning(val message: String) : ByzantineInviteMembersEvent()
}

data class ByzantineInviteMembersState(
    val members: List<InviteMemberUi> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val suggestionContacts: List<Contact> = emptyList(),
    val walletConstraints: WalletConstraints? = null,
    val interactingIndex: Int = -1,
    val inheritance: Inheritance? = null,
)

data class InviteMemberUi(
    val role: String,
    val name: String?,
    val email: String,
    val userId: String?,
    val isContact: Boolean = false,
    val err: String? = null,
    val isNewAdded: Boolean = false
) {
    companion object {
        val DEFAULT = InviteMemberUi(
            role = AssistedWalletRole.NONE.name,
            name = "",
            email = "",
            isNewAdded = true,
            userId = null
        )
    }
}

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
        isContact = isContact(),
        userId = user?.id
    )
}