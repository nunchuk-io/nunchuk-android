/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.components.tabs.services

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesInheritanceUseCase
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlanFlowUseCase
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUseCase
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUserDataUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.util.ByzantineGroupUtils
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.messages.usecase.message.GetOrCreateSupportRoomUseCase
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.byzantine.isKeyHolderWithoutKeyHolderLimited
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.isPremier
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.isByzantine
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.toGroupWalletType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.banner.GetAssistedWalletPageContentUseCase
import com.nunchuk.android.usecase.banner.GetBannerUseCase
import com.nunchuk.android.usecase.banner.SubmitEmailUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.InheritanceCheckUseCase
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServicesTabViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getLocalMembershipPlanFlowUseCase: GetLocalMembershipPlanFlowUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getInheritanceUseCase: GetInheritanceUseCase,
    private val getOrCreateSupportRoomUseCase: GetOrCreateSupportRoomUseCase,
    private val inheritanceCheckUseCase: InheritanceCheckUseCase,
    private val accountManager: AccountManager,
    private val getAssistedWalletPageContentUseCase: GetAssistedWalletPageContentUseCase,
    private val getBannerUseCase: GetBannerUseCase,
    private val submitEmailUseCase: SubmitEmailUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val calculateRequiredSignaturesInheritanceUseCase: CalculateRequiredSignaturesInheritanceUseCase,
    private val requestPlanningInheritanceUseCase: RequestPlanningInheritanceUseCase,
    private val requestPlanningInheritanceUserDataUseCase: RequestPlanningInheritanceUserDataUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<ServicesTabEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ServicesTabState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getLocalMembershipPlanFlowUseCase(Unit)
                .map { it.getOrElse { MembershipPlan.NONE } }
                .combine(getAssistedWalletIdsFlowUseCase(Unit)
                    .map { it.getOrElse { emptyList() } }
                    .distinctUntilChanged()
                ) { plan, wallets ->
                    plan to wallets
                }
                .collect { (plan, wallets) ->
                    _state.update { it.copy(assistedWallets = wallets) }
                    handleAssistedWallet(wallets, plan)
                }
        }
        viewModelScope.launch {
            getGroupsUseCase(Unit)
                .collect { result ->
                    val groups = result.getOrDefault(emptyList())
                    _state.update { it.copy(allGroups = groups) }
                    updateGroupInfo(groups)
                }
        }
    }

    private fun updateGroupInfo(groups: List<ByzantineGroup>) {
        val joinedGroups = groups.filter { byzantineGroupUtils.isPendingAcceptInvite(it).not() }
        val group2of4Multisigs =
            groups.filter { it.walletConfig.m == GroupWalletType.TWO_OF_FOUR_MULTISIG.m && it.walletConfig.n == GroupWalletType.TWO_OF_FOUR_MULTISIG.n }
        val group2Of3Or3of5Multisigs: List<ByzantineGroup>
        val sortedGroups: List<ByzantineGroup>
        if (group2of4Multisigs.isEmpty()) {
            group2Of3Or3of5Multisigs =
                groups.filter {
                    it.walletConfig.m == GroupWalletType.TWO_OF_THREE.m && it.walletConfig.n == GroupWalletType.TWO_OF_THREE.n
                            || it.walletConfig.m == GroupWalletType.THREE_OF_FIVE.m && it.walletConfig.n == GroupWalletType.THREE_OF_FIVE.n
                }
            sortedGroups = group2Of3Or3of5Multisigs.sortedWith(compareBy { group ->
                AssistedWalletRoleByOrder.valueOf(byzantineGroupUtils.getCurrentUserRole(group))
            })
        } else {
            sortedGroups = group2of4Multisigs.sortedWith(compareBy { group ->
                AssistedWalletRoleByOrder.valueOf(byzantineGroupUtils.getCurrentUserRole(group))
            })
        }
        _state.update { state ->
            state.copy(
                groups2of4Multisig = if (group2of4Multisigs.isEmpty()) emptyList() else sortedGroups,
                userRole = byzantineGroupUtils.getCurrentUserRole(
                    sortedGroups.firstOrNull()
                ),
                joinedGroups = joinedGroups.associateBy { it.id },
            )
        }
    }

    @Keep
    private enum class AssistedWalletRoleByOrder {
        MASTER, ADMIN, KEYHOLDER, KEYHOLDER_LIMITED, OBSERVER
    }

    private fun handleAssistedWallet(
        assistedWallets: List<AssistedWalletBrief>,
        plan: MembershipPlan
    ) {
        if (plan == MembershipPlan.NONE) {
            getNonSubscriberPageContent()
        } else {
            _state.update {
                it.copy(
                    plan = plan,
                    isPremiumUser = true,
                )
            }
            if (assistedWallets.isEmpty()) {
                viewModelScope.launch {
                    val bannerResult = getBannerUseCase(Unit)
                    _state.update {
                        it.copy(banner = bannerResult.getOrNull())
                    }
                }
            }
        }
    }

    fun getRowItems() = _state.value.initRowItems()

    fun getInheritance(walletId: String, token: String, groupId: String?) = viewModelScope.launch {
        getInheritanceUseCase(GetInheritanceUseCase.Param(walletId, groupId)).onSuccess {
            _event.emit(ServicesTabEvent.GetInheritanceSuccess(walletId, it, token, groupId))
        }.onFailure {
            _event.emit(ServicesTabEvent.ProcessFailure(it.message.orUnknownError()))
        }
    }

    private fun getNonSubscriberPageContent() = viewModelScope.launch {
        val pageResult = getAssistedWalletPageContentUseCase("")
        if (pageResult.isSuccess) {
            _state.update {
                it.copy(
                    plan = MembershipPlan.NONE,
                    isPremiumUser = false,
                    bannerPage = pageResult.getOrThrow()
                )
            }
        } else {
            _event.emit(ServicesTabEvent.ProcessFailure(pageResult.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun confirmPassword(
        walletId: String,
        password: String,
        item: ServiceTabRowItem
    ) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        _event.emit(ServicesTabEvent.Loading(true))
        val targetAction = when (item) {
            is ServiceTabRowItem.EmergencyLockdown -> TargetAction.EMERGENCY_LOCKDOWN.name
            is ServiceTabRowItem.CoSigningPolicies -> TargetAction.UPDATE_SERVER_KEY.name
            is ServiceTabRowItem.ViewInheritancePlan -> TargetAction.UPDATE_INHERITANCE_PLAN.name
            else -> throw IllegalArgumentException()
        }
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                targetAction = targetAction,
                password = password
            )
        )
        _event.emit(ServicesTabEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(
                ServicesTabEvent.CheckPasswordSuccess(
                    token = result.getOrThrow().orEmpty(),
                    walletId = walletId,
                    item = item,
                    groupId = assistedWalletManager.getGroupId(walletId)
                )
            )
        } else {
            _event.emit(ServicesTabEvent.ProcessFailure(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getServiceKey(token: String, walletId: String) = viewModelScope.launch {
        _event.emit(ServicesTabEvent.Loading(true))
        getWalletUseCase.execute(walletId)
            .map { it.wallet.signers }
            .map { signers -> signers.firstOrNull { it.type == SignerType.SERVER } }
            .flowOn(Dispatchers.IO)
            .onException {
                _event.emit(ServicesTabEvent.Loading(false))
                _event.emit(ServicesTabEvent.ProcessFailure(it.message.orUnknownError()))
            }
            .flowOn(Dispatchers.Main)
            .collect {
                _event.emit(ServicesTabEvent.Loading(false))
                it?.let {
                    _event.emit(
                        ServicesTabEvent.GetServerKeySuccess(
                            signer = it,
                            walletId = walletId,
                            token = token
                        )
                    )
                }
            }
    }

    fun checkInheritance() = viewModelScope.launch {
        _event.emit(ServicesTabEvent.Loading(true))
        val result = inheritanceCheckUseCase(Unit)
        _event.emit(ServicesTabEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(ServicesTabEvent.CheckInheritance(result.getOrThrow()))
        } else {
            _event.emit(ServicesTabEvent.ProcessFailure(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun isLoggedIn() = accountManager.isAccountExisted()

    fun getGroupStage(): MembershipStage {
        if (_state.value.assistedWallets.isNotEmpty()) return MembershipStage.DONE
        if (membershipStepManager.isNotConfig()) return MembershipStage.NONE
        return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }

    fun getOrCreateSupportRom() {
        viewModelScope.launch {
            _event.emit(ServicesTabEvent.Loading(true))
            val result = getOrCreateSupportRoomUseCase(Unit)
            _event.emit(ServicesTabEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(ServicesTabEvent.CreateSupportRoomSuccess(result.getOrThrow().roomId))
            } else {
                _event.emit(ServicesTabEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun openSetupInheritancePlan(walletId: String) {
        viewModelScope.launch {
            val groupId = assistedWalletManager.getGroupId(walletId)
            if (groupId != null) {
                _event.emit(ServicesTabEvent.Loading(true))
                val inheritance =
                    getInheritanceUseCase(GetInheritanceUseCase.Param(walletId, groupId))
                if (inheritance.getOrNull()?.status == InheritanceStatus.PENDING_APPROVAL) {
                    calculateRequiredSignatures(walletId, groupId)
                } else {
                    _event.emit(ServicesTabEvent.OpenSetupInheritancePlan(walletId, groupId))
                }
                _event.emit(ServicesTabEvent.Loading(false))
            } else {
                _event.emit(ServicesTabEvent.OpenSetupInheritancePlan(walletId, null))
            }
        }
    }

    private fun calculateRequiredSignatures(walletId: String, groupId: String) {
        viewModelScope.launch {
            _event.emit(ServicesTabEvent.Loading(true))
            val userData = requestPlanningInheritanceUserDataUseCase(
                RequestPlanningInheritanceUserDataUseCase.Param(
                    walletId = walletId,
                    groupId = groupId
                )
            )
            calculateRequiredSignaturesInheritanceUseCase(
                CalculateRequiredSignaturesInheritanceUseCase.Param(
                    walletId = walletId,
                    action = CalculateRequiredSignaturesAction.REQUEST_PLANNING,
                    groupId = groupId
                )
            ).onSuccess { resultCalculate ->
                requestPlanningInheritanceUseCase(
                    RequestPlanningInheritanceUseCase.Param(
                        userData = userData.getOrThrow(),
                        walletId = walletId,
                        groupId = groupId
                    )
                ).onSuccess {
                    _event.emit(
                        ServicesTabEvent.CalculateRequiredSignaturesSuccess(
                            type = resultCalculate.type,
                            walletId = walletId,
                            groupId = groupId,
                            userData = userData.getOrThrow(),
                            requiredSignatures = resultCalculate.requiredSignatures,
                            dummyTransactionId = it
                        )
                    )
                }.onFailure {
                    _event.emit(ServicesTabEvent.ProcessFailure(it.message.orUnknownError()))
                }
            }.onFailure {
                _event.emit(ServicesTabEvent.ProcessFailure(it.message.orUnknownError()))
            }
            _event.emit(ServicesTabEvent.Loading(false))
        }
    }

    fun submitEmail(email: String) {
        viewModelScope.launch {
            if (EmailValidator.valid(email).not()) {
                _event.emit(ServicesTabEvent.EmailInvalid)
                return@launch
            }
            _event.emit(ServicesTabEvent.Loading(true))
            val result = submitEmailUseCase(
                SubmitEmailUseCase.Param(
                    email = email,
                    bannerId = null,
                )
            )
            _event.emit(ServicesTabEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(ServicesTabEvent.OnSubmitEmailSuccess(email))
            } else {
                _event.emit(ServicesTabEvent.ProcessFailure(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    fun getEmail() = accountManager.getAccount().email

    fun getUnSetupInheritanceWallets(): List<AssistedWalletBrief> {
        val wallets = state.value.assistedWallets.filter { it.isSetupInheritance.not() && isInheritanceOwner(it.ext.inheritanceOwnerId) }
        return wallets.filter {
                it.groupId.isEmpty() || isAllowSetupInheritance(it)
            }
    }

    private fun isAllowSetupInheritance(wallet: AssistedWalletBrief): Boolean {
        return state.value.groups2of4Multisig.find { group -> group.id == wallet.groupId } != null
                && byzantineGroupUtils.getCurrentUserRole(state.value.joinedGroups[wallet.groupId]).toRole.isMasterOrAdmin
                && state.value.joinedGroups[wallet.groupId]?.walletConfig?.allowInheritance == true
    }

    /**
     * Get wallets that are able to setup inheritance or config server key policy
     * Limit to 2 of 4 multisig group if plan is byzantine
     * Limit to master or admin or keyholder
     * @param ignoreSetupInheritance: ignore setup inheritance or not
     */
    fun getWallets(ignoreSetupInheritance: Boolean = true): List<AssistedWalletBrief> {
        val wallets =
            if (ignoreSetupInheritance.not()) state.value.assistedWallets.filter { it.isSetupInheritance } else state.value.assistedWallets
        return if (state.value.plan.isByzantine()) {
            wallets.filter {
                state.value.groups2of4Multisig.find { group ->
                    group.id == it.groupId
                } != null && byzantineGroupUtils.getCurrentUserRole(state.value.joinedGroups[it.groupId]).toRole.isKeyHolderWithoutKeyHolderLimited
            }
        } else {
            wallets.filter {
                it.groupId.isEmpty() || byzantineGroupUtils.getCurrentUserRole(state.value.joinedGroups[it.groupId]).toRole.isKeyHolderWithoutKeyHolderLimited
            }
        }
    }

    private fun isInheritanceOwner(inheritanceOwnerId: String?): Boolean {
        return inheritanceOwnerId.isNullOrEmpty() || inheritanceOwnerId == accountManager.getAccount().id
    }

    /**
     * Get wallets with master or admin role
     */
    fun getAllowEmergencyLockdownWallets(): Pair<Boolean, List<AssistedWalletBrief>> {
        var hasLockedWallet = false
        val wallets = state.value.assistedWallets.filter {
            if (state.value.joinedGroups[it.groupId]?.isLocked == true) {
                hasLockedWallet = true
            }
            byzantineGroupUtils.getCurrentUserRole(state.value.joinedGroups[it.groupId]).toRole.isMasterOrAdmin && state.value.joinedGroups[it.groupId]?.isLocked == false
        }
        return hasLockedWallet to wallets
    }

    fun getGroupId(walletId: String): String? = assistedWalletManager.getGroupId(walletId)

    private fun isNoByzantineWallet(): Boolean {
        return isByzantine() && state.value.joinedGroups.all { it.value.isPendingWallet() }
    }

    fun isByzantine(): Boolean {
        return state.value.plan.isByzantine() || state.value.allGroups.isNotEmpty()
    }

    fun isShowClaimInheritanceLayout(): Boolean {
        if (state.value.plan == MembershipPlan.HONEY_BADGER || state.value.plan == MembershipPlan.IRON_HAND) return false
        if (state.value.allGroups.any { it.walletConfig.toGroupWalletType()?.isPremier() == true } || state.value.plan == MembershipPlan.BYZANTINE_PREMIER) return false
        if (state.value.userRole.toRole == AssistedWalletRole.OBSERVER) return true
        if (isNoByzantineWallet()) return true
        if (state.value.plan == MembershipPlan.NONE && state.value.joinedGroups.isEmpty()) return true
        return false
    }
}