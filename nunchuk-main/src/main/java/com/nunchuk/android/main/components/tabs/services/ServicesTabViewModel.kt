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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesInheritanceUseCase
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.messages.usecase.message.GetOrCreateSupportRoomUseCase
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.AssistedWalletRoleOrder
import com.nunchuk.android.model.byzantine.isKeyHolderWithoutKeyHolderLimited
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.containsByzantineOrFinney
import com.nunchuk.android.model.containsPersonalPlan
import com.nunchuk.android.model.isNonePlan
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.banner.GetAssistedWalletPageContentUseCase
import com.nunchuk.android.usecase.banner.GetBannerUseCase
import com.nunchuk.android.usecase.banner.SubmitEmailUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.InheritanceCheckUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ServicesTabViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getWalletUseCase: GetWalletUseCase,
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
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
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<ServicesTabEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ServicesTabState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .combine(getAssistedWalletIdsFlowUseCase(Unit)
                    .map { it.getOrElse { emptyList() } }
                    .distinctUntilChanged()
                ) { plans, wallets ->
                    plans to wallets
                }
                .collect { (plans, wallets) ->
                    _state.update { it.copy(assistedWallets = wallets.filter { wallet -> wallet.status != WalletStatus.REPLACED.name }) }
                    handleAssistedWallet(wallets, plans)
                }
        }
        viewModelScope.launch {
            getGroupsUseCase(Unit)
                .collect { result ->
                    val groups = result.getOrDefault(emptyList())
                    _state.update { it -> it.copy(accountId = accountManager.getAccount().id, allGroups = groups.associateWith { byzantineGroupUtils.getCurrentUserRole(it).toRole }) }
                    updateGroupInfo(groups)
                }
        }
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { assistedWallets ->
                    _state.update { it.copy(assistedWallets = assistedWallets.filter { wallet -> wallet.status != WalletStatus.REPLACED.name }) }
                    getRowItems()
                }
        }
    }

    private fun updateGroupInfo(groups: List<ByzantineGroup>) = viewModelScope.launch(ioDispatcher) {
        val joinedGroups = groups.filter { byzantineGroupUtils.isPendingAcceptInvite(it).not() }
        val allowInheritanceMultisigs = groups.filter { it.walletConfig.allowInheritance }
        val groupNotAllowInheritanceMultisigs: List<ByzantineGroup>
        val sortedGroups: List<ByzantineGroup>
        if (allowInheritanceMultisigs.isEmpty()) {
            groupNotAllowInheritanceMultisigs = groups.filter { it.walletConfig.allowInheritance.not() }
            sortedGroups = groupNotAllowInheritanceMultisigs.sortedWith(compareBy { group ->
                AssistedWalletRoleOrder.valueOf(byzantineGroupUtils.getCurrentUserRole(group))
            })
        } else {
            sortedGroups = allowInheritanceMultisigs.sortedWith(compareBy { group ->
                AssistedWalletRoleOrder.valueOf(byzantineGroupUtils.getCurrentUserRole(group))
            })
        }
        val isMasterHasNotCreatedWallet = groups.all { it.isPendingWallet() && byzantineGroupUtils.getCurrentUserRole(it) == AssistedWalletRole.MASTER.name }
        withContext(Dispatchers.Main) {
            _state.update { state ->
                state.copy(
                    allowInheritanceGroups = if (allowInheritanceMultisigs.isEmpty()) emptyList() else sortedGroups,
                    userRole = byzantineGroupUtils.getCurrentUserRole(sortedGroups.firstOrNull()),
                    joinedGroups = joinedGroups.associateBy { it.id },
                    isMasterHasNotCreatedWallet = isMasterHasNotCreatedWallet,
                )
            }
        }
    }

    private fun handleAssistedWallet(
        assistedWallets: List<AssistedWalletBrief>,
        plans: List<MembershipPlan>
    ) {
        if (plans.isNonePlan()) {
            getNonSubscriberPageContent()
        } else {
            _state.update {
                it.copy(
                    plans = plans,
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

    fun getRowItems() = viewModelScope.launch(ioDispatcher) {
        _event.emit(ServicesTabEvent.RowItems(_state.value.rowItems))
        val rowItems = _state.value.initRowItems()
        _event.emit(ServicesTabEvent.RowItems(rowItems))
        _state.update { it.copy(rowItems = rowItems) }
    }

    fun isPremiumUser() = state.value.isPremiumUser

    fun getInheritance(walletId: String, token: String, groupId: String?) = viewModelScope.launch {
        _event.emit(ServicesTabEvent.Loading(true))
        getInheritanceUseCase(GetInheritanceUseCase.Param(walletId, groupId)).onSuccess {
            _event.emit(ServicesTabEvent.GetInheritanceSuccess(walletId, it, token, groupId))
        }.onFailure {
            _event.emit(ServicesTabEvent.ProcessFailure(it.message.orUnknownError()))
        }
        _event.emit(ServicesTabEvent.Loading(false))
    }

    private fun getNonSubscriberPageContent() = viewModelScope.launch {
        val pageResult = getAssistedWalletPageContentUseCase("")
        if (pageResult.isSuccess) {
            _state.update {
                it.copy(
                    plans = arrayListOf(),
                    isPremiumUser = false,
                    bannerPage = pageResult.getOrThrow()
                )
            }
        } else {
            _event.emit(ServicesTabEvent.ProcessFailure(pageResult.exceptionOrNull()?.message.orUnknownError()))
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
            val groupId = state.value.assistedWallets.find { it.localId == walletId }?.groupId
            if (groupId.isNullOrEmpty().not()) {
                _event.emit(ServicesTabEvent.Loading(true))
                val inheritance =
                    getInheritanceUseCase(GetInheritanceUseCase.Param(walletId, groupId))
                if (inheritance.getOrNull()?.status == InheritanceStatus.PENDING_APPROVAL) {
                    calculateRequiredSignatures(walletId, groupId!!)
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
            calculateRequiredSignaturesInheritanceUseCase(
                CalculateRequiredSignaturesInheritanceUseCase.Param(
                    walletId = walletId,
                    action = CalculateRequiredSignaturesAction.REQUEST_PLANNING,
                    groupId = groupId
                )
            ).onSuccess { resultCalculate ->
                _event.emit(ServicesTabEvent.CalculateRequiredSignaturesSuccess(type = resultCalculate.type, walletId = walletId, groupId = groupId))
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
        return _state.value.getUnSetupInheritanceWallets()
    }

    fun getConfigServerKeyWallets(): List<AssistedWalletBrief> {
        val wallets = state.value.assistedWallets
        val allowCoSigningPolicyGroups = state.value.getGroupsAllowCoSigningPolicies()
        return wallets.filter { walletBrief ->
            walletBrief.groupId.isEmpty() || (allowCoSigningPolicyGroups.find { group ->
                group.id == walletBrief.groupId
            } != null && byzantineGroupUtils.getCurrentUserRole(state.value.joinedGroups[walletBrief.groupId]).toRole.isKeyHolderWithoutKeyHolderLimited)
        }
    }

    fun getViewClaimInheritanceWallets(): List<AssistedWalletBrief> {
        val wallets = state.value.assistedWallets.filter { it.isSetupInheritance }
        return wallets.filter {
                it.groupId.isEmpty() || (state.value.allowInheritanceGroups.find { group ->
                    group.id == it.groupId
                } != null && byzantineGroupUtils.getCurrentUserRole(state.value.joinedGroups[it.groupId]).toRole.isKeyHolderWithoutKeyHolderLimited)
            }
    }

    fun getActiveWalletsAndNoReplaced(): List<AssistedWalletBrief> {
        return state.value.assistedWallets.filter { wallet -> wallet.status == WalletStatus.ACTIVE.name }
    }

    /**
     * Get wallets with master or admin role or no group
     */
    fun getAllowEmergencyLockdownWallets(): Pair<Int, List<AssistedWalletBrief>> {
        var numOfLockedWallet = 0
        val wallets = state.value.assistedWallets.filter {
            if (state.value.joinedGroups[it.groupId]?.isLocked == true) {
                numOfLockedWallet ++
            }
            byzantineGroupUtils.getCurrentUserRole(state.value.joinedGroups[it.groupId]).toRole.isMasterOrAdmin || it.groupId.isEmpty()
        }
        return numOfLockedWallet to wallets
    }

    fun getLockdownWalletsIds(): List<String> {
        return state.value.assistedWallets.filter {
            state.value.joinedGroups[it.groupId]?.isLocked == true
        }.map { it.localId }
    }

    fun getGroupId(walletId: String): String? = assistedWalletManager.getGroupId(walletId)

    private fun isNoByzantineWallet(): Boolean {
        return isByzantine() && state.value.joinedGroups.all { it.value.isPendingWallet() }
    }

    fun isByzantine(): Boolean {
        return state.value.plans.containsByzantineOrFinney() || state.value.allGroups.isNotEmpty()
    }

    fun isShowClaimInheritanceLayout(): Boolean {
        if (state.value.plans.containsPersonalPlan()) return false
        if (state.value.allGroups.keys.any { it.isPremier() } || state.value.plans.contains(MembershipPlan.BYZANTINE_PREMIER)) return false
        if (state.value.userRole.toRole == AssistedWalletRole.OBSERVER) return true
        if (isNoByzantineWallet()) return true
        return (state.value.plans.isNonePlan()) && state.value.joinedGroups.isEmpty()
    }

    fun isOnChainWallet(walletId: String): Boolean {
        return assistedWalletManager.getBriefWallet(walletId)?.walletType == WalletType.MINISCRIPT.name
    }
}