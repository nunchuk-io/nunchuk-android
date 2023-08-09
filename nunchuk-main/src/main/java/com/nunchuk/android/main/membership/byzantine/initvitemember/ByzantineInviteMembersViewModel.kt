package com.nunchuk.android.main.membership.byzantine.initvitemember

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesEditGroupMemberUseCase
import com.nunchuk.android.core.domain.membership.EditGroupMemberUseCase
import com.nunchuk.android.core.domain.membership.EditGroupMemberUserDataUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.main.membership.model.GroupWalletType
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isKeyHolder
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.usecase.GetWalletConstraintsUseCase
import com.nunchuk.android.usecase.membership.CreateGroupWalletUseCase
import com.nunchuk.android.utils.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountManager: AccountManager,
    private val getContactsUseCase: GetContactsUseCase,
    private val createGroupWalletUseCase: CreateGroupWalletUseCase,
    private val editGroupMemberUseCase: EditGroupMemberUseCase,
    private val calculateRequiredSignaturesEditGroupMemberUseCase: CalculateRequiredSignaturesEditGroupMemberUseCase,
    private val editGroupMemberUserDataUseCase: EditGroupMemberUserDataUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val getWalletConstraintsUseCase: GetWalletConstraintsUseCase
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
            // call api get members
            val members = args.members.toList().map {
                it.toInviteMemberUi()
            }
            _state.update { it.copy(members = members) }
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
                    _state.update { it.copy(contacts = contacts.filter { it.loginType != SignInMode.PRIMARY_KEY.name }) }
                }
        }
    }

    private fun addMasterMember() {
        val account = accountManager.getAccount()
        val masterMember = InviteMemberUi(
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
                members = _state.value.members + InviteMemberUi(
                    role = AssistedWalletRole.NONE.name,
                    name = "",
                    email = "",
                    isNewAdded = true
                )
            )
        }
    }

    fun removeMember(index: Int) {
        _state.value.members[index].email
        _state.update {
            it.copy(
                members = _state.value.members.filterIndexed { i, _ -> i != index },
            )
        }
    }

    fun updateMember(
        index: Int,
        name: String? = null,
        email: String? = null,
        role: String? = null,
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
        }
        viewModelScope.launch {
            if (role != null) {
                if (countKeyholderRole(index, role) > _state.value.walletConstraints?.maximumKeyholder.orDefault(3)) {
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
                                err = if (email != null) getError(email) else member.err
                            )
                        } else {
                            member
                        }
                    }
                )
            }
        }
    }

    private fun emitWithDelay(event: ByzantineInviteMembersEvent) {
        viewModelScope.launch {
            delay(50)
            _event.emit(event)
        }
    }

    private fun countKeyholderRole(index: Int, newRole: String): Int {
        val member = _state.value.members[index]
        val countRole = _state.value.members.count { it.role.toRole.isKeyHolder }
        return if (member.role.toRole.isKeyHolder) {
            if (newRole.toRole.isKeyHolder) {
                countRole
            } else {
                countRole - 1
            }
        } else {
            if (newRole.toRole.isKeyHolder) {
                countRole + 1
            } else {
                countRole
            }
        }
    }

    private fun getError(email: String?): String {
        if (email == null) return ""
        if (email in getMemberEmails()) {
            return context.getString(R.string.nc_duplicate_email_addresses)
        }
        return ""
    }

    fun interactingMemberIndex(index: Int) {
        _state.update {
            it.copy(
                interactingIndex = index
            )
        }
    }

    fun enableContinueButton(): Boolean {
        val members = _state.value.members
        val isAllMemberValid = members.isNotEmpty() && members.all {
            it.role != AssistedWalletRole.NONE.name && EmailValidator.valid(
                it.email
            ) && it.err.isNullOrEmpty()
        }
        return if (args.flow == ByzantineMemberFlow.SETUP) {
            isAllMemberValid
        } else {
            members.toString() != existingData && isAllMemberValid
        }
    }

    private fun getMemberEmails(): HashSet<String> {
        return _state.value.members.filter { EmailValidator.valid(it.email) }
            .map { it.email }
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
                        member.copy(role = AssistedWalletRole.NONE.name)
                    } else {
                        member
                    }
                }
            )
        }
    }

    fun hasAdminRole(): Boolean {
        return _state.value.members.any { it.role == AssistedWalletRole.ADMIN.name }
    }

    fun createGroup() = viewModelScope.launch {
        _event.emit(ByzantineInviteMembersEvent.Loading(true))
        val result = createGroupWalletUseCase(
            CreateGroupWalletUseCase.Param(
                members = _state.value.members.map {
                    AssistedMember(
                        role = it.role,
                        name = it.name,
                        email = it.email
                    )
                },
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
        val members = stateValue.members.map { it.toAssistedMember() }
        _event.emit(ByzantineInviteMembersEvent.Loading(true))
        val resultCalculate = calculateRequiredSignaturesEditGroupMemberUseCase(
            CalculateRequiredSignaturesEditGroupMemberUseCase.Param(
                groupId = args.groupId,
                members = members,
            )
        )
        val resultUserData =
            editGroupMemberUserDataUseCase(EditGroupMemberUserDataUseCase.Param(members = members))
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
                members = state.members.map { it.toAssistedMember() },
                groupId = args.groupId,
                confirmCodeToken = confirmCodeToken,
                confirmCodeNonce = confirmCodeNonce
            )
        )
        _event.emit(ByzantineInviteMembersEvent.Loading(false))
        if (result.isSuccess) {
            val members = result.getOrThrow().members.map {
                it.toInviteMemberUi()
            }
            existingData = members.toString()
            _state.update {
                it.copy(
                    members = members
                )
            }
            _event.emit(ByzantineInviteMembersEvent.EditGroupMemberSuccess(result.getOrThrow().members))
        } else {
            _event.emit(ByzantineInviteMembersEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}