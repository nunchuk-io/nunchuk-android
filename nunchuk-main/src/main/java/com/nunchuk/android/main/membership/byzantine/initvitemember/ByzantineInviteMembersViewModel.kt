package com.nunchuk.android.main.membership.byzantine.initvitemember

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesEditGroupMemberUseCase
import com.nunchuk.android.core.domain.membership.EditGroupMemberUseCase
import com.nunchuk.android.core.domain.membership.EditGroupMemberUserDataUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
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
            val groupWalletType = args.groupType.toGroupWalletType()
            result.getOrDefault(emptyList())
                .find { it.walletConfig.m == groupWalletType.m && it.walletConfig.n == groupWalletType.n }
                ?.let { walletConstraints ->
                    _state.update { it.copy(walletConstraints = walletConstraints) }
                }
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
        _state.update {
            it.copy(
                members = _state.value.members + masterMember
            )
        }
    }

    fun addMember() {
        _state.update {
            it.copy(
                members = _state.value.members + AssistedMember(
                    role = AssistedWalletRole.NONE.name,
                    name = "",
                    email = ""
                )
            )
        }
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
        role: String? = null,
        loginType: String? = null
    ) {
        if (email != null) {
            viewModelScope.launch(ioDispatcher) {
                val suggestionContacts = _state.value.contacts.filter {
                    it.email !in getMemberEmails() && it.email.contains(email)
                }
                _state.update {
                    it.copy(
                        suggestionContacts = suggestionContacts
                    )
                }
            }
            if (email in getMemberEmails()) {
                return
            }
        }
        viewModelScope.launch {
            if (role != null) {
                if (countKeyholderRole() == _state.value.walletConstraints?.maximumKeyholder) {
                    emitWithDelay(ByzantineInviteMembersEvent.LimitKeyholderRoleWarning)
                    return@launch
                }
            }
            _state.update {
                it.copy(
                    members = _state.value.members.mapIndexed { i, member ->
                        if (i == index) {
                            member.copy(
                                name = name ?: member.name,
                                email = email ?: member.email,
                                role = role ?: member.role,
                                loginType = loginType ?: member.loginType
                            )
                        } else {
                            member
                        }
                    }
                )
            }
            if (role != null && role == AssistedWalletRole.ADMIN.name) {
                val member = _state.value.members[index]
                if (member.email.isNotBlank()) {
                    if (isPrimaryKeyAdminRole(member.email)) {
                        emitWithDelay(ByzantineInviteMembersEvent.PrimaryKeyAdminRoleWarning)
                    }
                }
            }
        }
    }

    fun interactingMemberIndex(index: Int) {
        _state.update {
            it.copy(
                interactingIndex = index
            )
        }
    }

    fun selectContact(email: String, isRemove: Boolean = false) {
        _state.update {
            it.copy(
                selectContacts = _state.value.selectContacts.apply {
                    if (isRemove) remove(email) else add(email)
                }
            )
        }
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
        _state.update {
            it.copy(
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
    }

    fun countKeyholderRole(): Int {
        return _state.value.members.count { it.role == AssistedWalletRole.KEYHOLDER.name || it.role == AssistedWalletRole.ADMIN.name }
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

    private fun emitWithDelay(event: ByzantineInviteMembersEvent) {
        viewModelScope.launch {
            delay(50)
            _event.emit(event)
        }
    }

    fun hasAdminRole(): Boolean {
        return _state.value.members.any { it.role == AssistedWalletRole.ADMIN.name }
    }

    fun createGroup() = viewModelScope.launch {
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