package com.nunchuk.android.main.membership.byzantine.initvitemember

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.membership.*
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.main.membership.model.GroupWalletType
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.usecase.GetWalletConstraintsUseCase
import com.nunchuk.android.usecase.membership.CreateGroupWalletUseCase
import com.nunchuk.android.utils.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ByzantineInviteMembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountManager: AccountManager,
    private val getContactsUseCase: GetContactsUseCase,
    private val createGroupWalletUseCase: CreateGroupWalletUseCase,
    private val getWalletConstraintsUseCase: GetWalletConstraintsUseCase,
    private val editGroupMemberUseCase: EditGroupMemberUseCase,
    private val calculateRequiredSignaturesEditGroupMemberUseCase: CalculateRequiredSignaturesEditGroupMemberUseCase,
    private val editGroupMemberUserDataUseCase: EditGroupMemberUserDataUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<ByzantineInviteMembersEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ByzantineInviteMembersState())
    val state = _state.asStateFlow()

    private val args = ByzantineInviteMembersFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private var existingData: String? = null
    private var verifyToken: String? = null

    init {
        getContacts()
        getWalletConstraints()
        if (args.flow == ByzantineMemberFlow.EDIT) {
            val members = args.members.toList()
            _state.value = _state.value.copy(
                members = members, preMembers = members
            )
            existingData = members.toString()
        } else {
            addMasterMember()
        }
    }

    private fun getWalletConstraints() {
        viewModelScope.launch {
            val result = getWalletConstraintsUseCase(Unit)
            _state.update { it.copy(walletConstraints = result.getOrNull()) }
        }
    }

    private fun getContacts() {
        viewModelScope.launch {
            getContactsUseCase.execute()
                .catch { _state.update { it.copy(contacts = emptyList()) } }
                .collect { contacts ->
                    _state.update { it.copy(contacts = contacts) }
                }
        }
    }

    private fun addMasterMember() {
        val account = accountManager.getAccount()
        val masterMember = AssistedMember(
            role = AssistedWalletRole.MASTER.name,
            name = account.name,
            email = account.email
        )
        _state.value = _state.value.copy(
            members = _state.value.members + masterMember
        )
    }

    fun addMember() {
        _state.value = _state.value.copy(
            members = _state.value.members + AssistedMember(
                role = AssistedWalletRole.NONE.name,
                name = "",
                email = ""
            )
        )
    }

    fun removeMember(index: Int) {
        val email = _state.value.members[index].email
        selectContact(email, isRemove = true)
        _state.value = _state.value.copy(
            members = _state.value.members.filterIndexed { i, _ -> i != index },
        )
        if (args.flow == ByzantineMemberFlow.EDIT) {
            _state.value = _state.value.copy(
                preMembers = _state.value.preMembers.filter { it.email != email },
            )
        }
    }

    fun updateMember(
        index: Int,
        name: String? = null,
        email: String? = null,
        role: String? = null
    ) {
        if (email != null) {
            viewModelScope.launch(ioDispatcher) {
                val suggestionContacts = _state.value.contacts.filter {
                    it.email !in getMemberEmails() && it.email.contains(email)
                }
                _state.value = _state.value.copy(
                    suggestionContacts = suggestionContacts
                )
            }
            if (email in getMemberEmails()) {
                return
            }
        }

        if (role != null) {
            viewModelScope.launch {
                if (countKeyholderRole() == _state.value.walletConstraints?.maximumKeyholder) {
                    _event.emit(ByzantineInviteMembersEvent.LimitKeyholderRoleWarning)
                    return@launch
                }
            }
        }

        _state.value = _state.value.copy(
            members = _state.value.members.mapIndexed { i, member ->
                if (i == index) {
                    member.copy(
                        name = name ?: member.name,
                        email = email ?: member.email,
                        role = role ?: member.role
                    )
                } else {
                    member
                }
            }
        )

        if (role != null && role == AssistedWalletRole.ADMIN.name) {
            val member = _state.value.members[index]
            if (member.email.isNotBlank()) {
                viewModelScope.launch {
                    if (isPrimaryKeyAdminRole(member.email)) {
                        _event.emit(ByzantineInviteMembersEvent.PrimaryKeyAdminRoleWarning)
                    }
                }
            }
        }
    }

    fun interactingMemberIndex(index: Int) {
        _state.value = _state.value.copy(
            interactingIndex = index
        )
    }

    fun selectContact(email: String, isRemove: Boolean = false) {
        _state.value = _state.value.copy(
            selectContacts = _state.value.selectContacts.apply {
                if (isRemove) remove(email) else add(email)
            }
        )
    }

    fun enableContinueButton(): Boolean {
        val members = _state.value.members
        val isAllMemberValid = members.isNotEmpty() && members.all {
            it.role != AssistedWalletRole.NONE.name && EmailValidator.valid(
                it.email
            )
        }
        return if (args.flow == ByzantineMemberFlow.SETUP) {
            isAllMemberValid
        } else {
            members.toString() != existingData && isAllMemberValid
        }
    }

    private fun getMemberEmails(): HashSet<String> {
        return _state.value.members.filter { EmailValidator.valid(it.email) }.map { it.email }
            .toHashSet()
    }

    fun getInteractingMemberIndex(): Int {
        return _state.value.interactingIndex
    }

    fun clearAdminRole() {
        _state.value = _state.value.copy(
            members = _state.value.members.mapIndexed { i, member ->
                if (member.role == AssistedWalletRole.ADMIN.name) {
                    member.copy(
                        role = AssistedWalletRole.NONE.name
                    )
                } else {
                    member
                }
            }
        )
    }

    fun countKeyholderRole(): Int {
        return _state.value.members.count { it.role == AssistedWalletRole.KEYHOLDER.name || it.role == AssistedWalletRole.MASTER.name }
    }

    private fun isPrimaryKeyAdminRole(email: String): Boolean {
        _state.value.contacts.forEach { contact ->
            if (email == contact.email
                && contact.isLoginInPrimaryKey()
                && _state.value.members.any { member -> member.email == email && member.role == AssistedWalletRole.ADMIN.name }
            ) {
                return true
            }
        }
        return false
    }

    fun hasAdminRole(): Boolean {
        return _state.value.members.any { it.role == AssistedWalletRole.ADMIN.name }
    }

    fun createGroupWallet() = viewModelScope.launch {
        _event.emit(ByzantineInviteMembersEvent.Loading(true))
        val result = createGroupWalletUseCase(
            CreateGroupWalletUseCase.Param(
                members = _state.value.members,
                m = args.groupType.toGroupWalletType().m,
                n = args.groupType.toGroupWalletType().n,
                allowInheritance = args.groupType == GroupWalletType.TWO_OF_FOUR_MULTISIG.name,
                requiredServerKey = args.groupType == GroupWalletType.TWO_OF_FOUR_MULTISIG.name,
                setupPreference = args.setupPreference,
            )
        )
        _event.emit(ByzantineInviteMembersEvent.Loading(false))
        if (result.isSuccess) {
            val groupId = result.getOrThrow().id
            _event.emit(ByzantineInviteMembersEvent.CreateGroupWalletSuccess(groupId))
        } else {
            _event.emit(ByzantineInviteMembersEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private suspend fun calculateRequiredSignatures() {
        val stateValue = _state.value
        _event.emit(ByzantineInviteMembersEvent.Loading(true))
        val resultCalculate = calculateRequiredSignaturesEditGroupMemberUseCase(
            CalculateRequiredSignaturesEditGroupMemberUseCase.Param(
                groupId = args.groupId,
                members = stateValue.members,
            )
        )
        val resultUserData =
            editGroupMemberUserDataUseCase(EditGroupMemberUserDataUseCase.Param(members = stateValue.members))
        val userData = resultUserData.getOrThrow()
        _event.emit(ByzantineInviteMembersEvent.Loading(false))
        if (resultCalculate.isSuccess) {
            _event.emit(
                ByzantineInviteMembersEvent.CalculateRequiredSignaturesSuccess(
                    type = resultCalculate.getOrThrow().type,
                    userData = userData,
                    requiredSignatures = resultCalculate.getOrThrow().requiredSignatures
                )
            )
        } else {
            _event.emit(ByzantineInviteMembersEvent.Error(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun confirmPassword(password: String) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        _event.emit(ByzantineInviteMembersEvent.Loading(true))
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                targetAction = TargetAction.EDIT_GROUP_MEMBERS.name,
                password = password
            )
        )
        _event.emit(ByzantineInviteMembersEvent.Loading(false))
        if (result.isSuccess) {
            verifyToken = result.getOrThrow().orEmpty()
            calculateRequiredSignatures()
        } else {
            _event.emit(ByzantineInviteMembersEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun editGroupMember(
        signatures: HashMap<String, String>,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String
    ) = viewModelScope.launch {
        if (verifyToken.isNullOrEmpty()) return@launch
        val state = _state.value
        _event.emit(ByzantineInviteMembersEvent.Loading(true))
        val result = editGroupMemberUseCase(
            EditGroupMemberUseCase.Param(
                signatures = signatures,
                verifyToken = verifyToken!!,
                securityQuestionToken = securityQuestionToken,
                members = state.members,
                groupId = args.groupId,
                confirmCodeToken = confirmCodeToken,
                confirmCodeNonce = confirmCodeNonce
            )
        )
        _event.emit(ByzantineInviteMembersEvent.Loading(false))
        if (result.isSuccess) {
            existingData = _state.value.members.toString()
            val preMembers = result.getOrThrow().members.map {
                AssistedMember(
                    email = it.user?.email ?: "",
                    role = it.role,
                    name = it.user?.name ?: "",
                    membershipId = it.membershipId
                )
            }
            _state.value = _state.value.copy(preMembers = preMembers, members = preMembers)
            _event.emit(ByzantineInviteMembersEvent.EditGroupMemberSuccess(result.getOrThrow().members))
        } else {
            _event.emit(ByzantineInviteMembersEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}