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

package com.nunchuk.android.main.components.tabs.wallet

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.data.model.DeeplinkInfo
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetNfcCardStatusUseCase
import com.nunchuk.android.core.domain.GetRemotePriceConvertBTCUseCase
import com.nunchuk.android.core.domain.IsShowNfcUniversalUseCase
import com.nunchuk.android.core.domain.JoinFreeGroupWalletByIdUseCase
import com.nunchuk.android.core.domain.membership.GetServerWalletsUseCase
import com.nunchuk.android.core.domain.membership.UpdateExistingKeyUseCase
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.DeeplinkHolder
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.listener.GroupDeleteListener
import com.nunchuk.android.listener.GroupSandboxListener
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GetTapSignerStatusSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GoToSatsCardScreen
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.Loading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NeedSetupSatsCard
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NfcLoading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.SatsCardUsedUp
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowErrorEvent
import com.nunchuk.android.model.ConnectionStatusHelper
import com.nunchuk.android.model.DEFAULT_FEE
import com.nunchuk.android.model.FreeRateOption
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.containsPersonalPlan
import com.nunchuk.android.model.isAllowSetupInheritance
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.setting.HomeDisplaySetting
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.model.wallet.WalletOrder
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetAllWalletsUseCase
import com.nunchuk.android.usecase.GetDefaultFeeUseCase
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetHomeDisplaySettingUseCase
import com.nunchuk.android.usecase.GetLocalCurrencyUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.MigrateHomeDisplaySettingUseCase
import com.nunchuk.android.usecase.SetHasWalletInGuestModeUseCase
import com.nunchuk.android.usecase.banner.GetBannerUseCase
import com.nunchuk.android.usecase.byzantine.GetListGroupWalletKeyHealthStatusUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberAcceptRequestUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberDenyRequestUseCase
import com.nunchuk.android.usecase.byzantine.SyncDeletedWalletUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.campaign.GetCurrentCampaignUseCase
import com.nunchuk.android.usecase.campaign.GetLocalCurrentCampaignUseCase
import com.nunchuk.android.usecase.campaign.GetLocalReferrerCodeUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetDeprecatedGroupWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetPendingGroupsSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.UpdateGroupSandboxConfigUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.GetPendingWalletNotifyCountUseCase
import com.nunchuk.android.usecase.membership.GetPersonalMembershipStepUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.user.IsHideUpsellBannerUseCase
import com.nunchuk.android.usecase.wallet.GetWalletOrderListUseCase
import com.nunchuk.android.usecase.wallet.InsertWalletOrderListUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.room.model.Membership
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
internal class WalletsViewModel @Inject constructor(
    private val getAllWalletsUseCase: GetAllWalletsUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val getNfcCardStatusUseCase: GetNfcCardStatusUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val accountManager: AccountManager,
    private val getUserSubscriptionUseCase: GetUserSubscriptionUseCase,
    private val getServerWalletsUseCase: GetServerWalletsUseCase,
    private val getInheritanceUseCase: GetInheritanceUseCase,
    private val getBannerUseCase: GetBannerUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getLocalCurrencyUseCase: GetLocalCurrencyUseCase,
    private val getRemotePriceConvertBTCUseCase: GetRemotePriceConvertBTCUseCase,
    private val pushEventManager: PushEventManager,
    isShowNfcUniversalUseCase: IsShowNfcUniversalUseCase,
    isHideUpsellBannerUseCase: IsHideUpsellBannerUseCase,
    private val syncGroupWalletsUseCase: SyncGroupWalletsUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val groupMemberAcceptRequestUseCase: GroupMemberAcceptRequestUseCase,
    private val groupMemberDenyRequestUseCase: GroupMemberDenyRequestUseCase,
    private val getPendingWalletNotifyCountUseCase: GetPendingWalletNotifyCountUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val syncDeletedWalletUseCase: SyncDeletedWalletUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getListGroupWalletKeyHealthStatusUseCase: GetListGroupWalletKeyHealthStatusUseCase,
    private val cardIdManager: CardIdManager,
    private val getPersonalMembershipStepUseCase: GetPersonalMembershipStepUseCase,
    private val updateExistingKeyUseCase: UpdateExistingKeyUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val getLocalCurrentCampaignUseCase: GetLocalCurrentCampaignUseCase,
    private val getLocalReferrerCodeUseCase: GetLocalReferrerCodeUseCase,
    private val getCurrentCampaignUseCase: GetCurrentCampaignUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val getHomeDisplaySettingUseCase: GetHomeDisplaySettingUseCase,
    private val sessionHolder: SessionHolder,
    private val migrateHomeDisplaySettingUseCase: MigrateHomeDisplaySettingUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val getPendingGroupsSandboxUseCase: GetPendingGroupsSandboxUseCase,
    private val getGroupWalletsUseCase: GetGroupWalletsUseCase,
    private val joinFreeGroupWalletByIdUseCase: JoinFreeGroupWalletByIdUseCase,
    private val deeplinkHolder: DeeplinkHolder,
    private val getDeprecatedGroupWalletsUseCase: GetDeprecatedGroupWalletsUseCase,
    private val getDefaultFeeUseCase: GetDefaultFeeUseCase,
    private val getWalletOrderListUseCase: GetWalletOrderListUseCase,
    private val insertWalletOrderListUseCase: InsertWalletOrderListUseCase,
    private val setHasWalletInGuestModeUseCase: SetHasWalletInGuestModeUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val updateGroupSandboxConfigUseCase: UpdateGroupSandboxConfigUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletsState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<WalletsEvent>()
    val event = _event.asSharedFlow()

    private val keyPolicyMap = hashMapOf<String, KeyPolicy>()

    val isShownNfcUniversal = isShowNfcUniversalUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val isHideUpsellBanner = isHideUpsellBannerUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var isRetrievingAlert = AtomicBoolean(false)
    private var isRetrievingKeyHealthStatus = AtomicBoolean(false)

    private var walletsRequestKey = ""

    private fun getState() = state.value

    private var loadWalletJob: Job? = null
    private var insertWalletOrderJob: Job? = null

    private val _walletOrderMap = getWalletOrderListUseCase(Unit)
        .map { it.getOrDefault(emptyList()) }
        .map { it.associateBy { order -> order.walletId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { assistedWallets ->
                    _state.update { it.copy(assistedWallets = assistedWallets) }
                    mapGroupWalletUi()
                    checkWalletsRequestKey(assistedWallets) {
                        checkInheritance(assistedWallets)
                        getKeyHealthStatus()
                    }
                }
        }
        checkMemberMembership()
        viewModelScope.launch {
            membershipStepManager.remainingTime.collect { remainingTime ->
                _state.update { it.copy(remainingTime = remainingTime) }
            }
        }
        viewModelScope.launch {
            isHideUpsellBanner.collect { isHideUpsellBanner ->
                _state.update { it.copy(isHideUpsellBanner = isHideUpsellBanner) }
            }
        }
        viewModelScope.launch {
            getLocalCurrencyUseCase(Unit).distinctUntilChanged().collect {
                LOCAL_CURRENCY = it.getOrDefault(USD_CURRENCY)
                getRemotePriceConvertBTCUseCase(Unit)
            }
        }
        viewModelScope.launch {
            getDefaultFeeUseCase(Unit).collect {
                DEFAULT_FEE = it.getOrDefault(FreeRateOption.ECONOMIC.ordinal)
            }
        }
        viewModelScope.launch {
            getPersonalMembershipStepUseCase(Unit).map {
                it.getOrElse { emptyList() }
            }.distinctUntilChanged().collect { personalSteps ->
                _state.update { it.copy(personalSteps = personalSteps) }
                mapGroupWalletUi()
            }
        }
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                when (event) {
                    is PushEvent.WalletCreate -> {
                        if (event.groupId.isEmpty()) {
                            // personal wallet
                            syncDraftWalletUseCase("")
                        } else if (!getState().wallets.any { it.wallet.id == event.walletId }) {
                            getServerWalletsUseCase(Unit).onSuccess {
                                if (it.isNeedReload) {
                                    retrieveData()
                                }
                            }
                        }
                    }

                    is PushEvent.DraftResetWallet -> {
                        if (event.groupId.isEmpty()) {
                            // personal wallet
                            syncDraftWalletUseCase("")
                        } else {
                            syncGroupWalletsUseCase(Unit).onSuccess { shouldReload ->
                                if (shouldReload) retrieveData()
                            }
                        }
                    }

                    is PushEvent.GroupMembershipRequestCreated -> {
                        if (!getState().allGroups.any { it.id == event.groupId }) {
                            syncGroupWalletsUseCase(Unit).onSuccess { shouldReload ->
                                if (shouldReload) retrieveData()
                            }
                        }
                    }

                    is PushEvent.GroupEmergencyLockdownStarted -> syncGroupWallets(event.walletId)

                    is PushEvent.GroupWalletCreated -> syncGroupWallets(event.walletId)

                    is PushEvent.PrimaryOwnerUpdated -> syncGroupWallets(event.walletId)

                    is PushEvent.WalletChanged, is PushEvent.SignedChanged -> {
                        retrieveData()
                    }

                    is PushEvent.InheritanceEvent -> {
                        if (event.isCancelled) {
                            val oldInheritance = getState().inheritances[event.walletId]
                            if (oldInheritance != null) {
                                _state.update { state ->
                                    state.copy(
                                        inheritances = state.inheritances.toMutableMap().apply {
                                            put(event.walletId, InheritanceStatus.PENDING_CREATION)
                                        },
                                    )
                                }
                            }
                        } else {
                            val assistedWallets =
                                getState().assistedWallets.filter { it.localId == event.walletId }
                            if (assistedWallets.isNotEmpty()) {
                                checkInheritance(assistedWallets)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
        syncGroup()
        viewModelScope.launch {
            syncDeletedWalletUseCase(Unit).onSuccess { shouldReload ->
                if (shouldReload) retrieveData()
            }
        }
        viewModelScope.launch {
            getGroupsUseCase(Unit).distinctUntilChanged().collect {
                val groups = it.getOrDefault(emptyList())
                _state.update { it.copy(allGroups = groups) }
                updateBadge()
                getKeyHealthStatus()
                mapGroupWalletUi()
            }
        }
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .map { it.getOrNull() }
                .collect { setting ->
                    _state.update {
                        it.copy(
                            walletSecuritySetting = setting ?: WalletSecuritySetting.DEFAULT
                        )
                    }
                }
        }
        viewModelScope.launch {
            if (accountManager.getAccount().id.isEmpty()) {
                getUserProfileUseCase(Unit)
            }
        }
        getReferrerCode()
        viewModelScope.launch {
            state.map { it.plans.orEmpty().containsPersonalPlan() }
                .filter { true }
                .distinctUntilChanged()
                .collect {
                    syncDraftWalletUseCase("")
                }
        }
        viewModelScope.launch { // Migrate home display setting
            migrateHomeDisplaySettingUseCase(Unit)
        }
        viewModelScope.launch {
            getHomeDisplaySettingUseCase(Unit)
                .map { it.getOrDefault(HomeDisplaySetting()) }
                .collect { setting ->
                    _state.update {
                        it.copy(
                            homeDisplaySetting = setting
                        )
                    }
                }
        }
        listenGroupSandbox()
        listenGroupDelete()
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrElse { Chain.MAIN } }
                .distinctUntilChanged()
                .collect { chain ->
                    _state.update { it.copy(chain = chain) }
                }
        }
        viewModelScope.launch {
            ConnectionStatusHelper.blockChainStatus
                .collect { syncStatus ->
                    _state.update { it.copy(connectionStatus = syncStatus?.status) }
                }
        }
        viewModelScope.launch {
            deeplinkHolder.groupLinkInfo.collect {
                if (it != null && it.groupId.isNotEmpty()) {
                    joinGroupWallet(it)
                }
            }
        }
    }

    private fun listenGroupSandbox() {
        viewModelScope.launch {
            GroupSandboxListener.getGroupFlow().collect { groupSandbox ->
                if (groupSandbox.finalized) {
                    val pendingGroupSandboxes =
                        getState().pendingGroupSandboxes.filter { it.id != groupSandbox.id }
                    _state.update { it.copy(pendingGroupSandboxes = pendingGroupSandboxes) }
                    retrieveData()
                }
            }
        }
    }

    private fun listenGroupDelete() {
        viewModelScope.launch {
            GroupDeleteListener.groupDeleteFlow.collect { groupId ->
                val pendingGroupSandboxes =
                    getState().pendingGroupSandboxes.filter { it.id != groupId }
                _state.update { it.copy(pendingGroupSandboxes = pendingGroupSandboxes) }
                mapGroupWalletUi()
            }
        }
    }

    private fun joinGroupWallet(info: DeeplinkInfo) = viewModelScope.launch {
        delay(2000)
        val groupId = info.groupId
        joinFreeGroupWalletByIdUseCase(groupId)
            .onSuccess {
                _event.emit(WalletsEvent.JoinFreeGroupWalletSuccess(it.id))
            }.onFailure {
                val errorCode = it.nativeErrorCode()
                if (errorCode == NativeErrorCode.GROUP_WALLET_JOINED) {
                    _event.emit(WalletsEvent.JoinFreeGroupWalletSuccess(groupId))
                } else {
                    _event.emit(WalletsEvent.JoinFreeGroupWalletFailed)
                }
            }
        deeplinkHolder.clearGroupInfo()
    }

    private fun getCampaign() {
        viewModelScope.launch {
            getLocalCurrentCampaignUseCase(Unit).map { it.getOrNull() }.collect { campaign ->
                _state.update { it.copy(campaign = campaign) }
            }
        }
    }

    private fun getReferrerCode() {
        viewModelScope.launch {
            getLocalReferrerCodeUseCase(Unit).map { it.getOrNull() }.distinctUntilChanged()
                .collect { localReferrerCode ->
                    _state.update { it.copy(localReferrerCode = localReferrerCode) }
                    getCurrentCampaignUseCase(GetCurrentCampaignUseCase.Param(accountManager.getAccount().email.ifEmpty { localReferrerCode?.email }))
                }
        }
    }

    private fun syncGroup() {
        viewModelScope.launch {
            syncGroupWalletsUseCase(Unit).onSuccess { shouldReload ->
                if (shouldReload) retrieveData()
            }
        }
    }

    fun reloadMembership() {
        viewModelScope.launch {
            delay(1000L)
            checkMemberMembership()
        }
    }

    private fun syncGroupWallets(walletId: String) {
        viewModelScope.launch {
            if (!getState().wallets.any { it.wallet.id == walletId }) {
                syncGroupWalletsUseCase(Unit).onSuccess { shouldReload ->
                    if (shouldReload) retrieveData()
                }
            }
        }
    }

    private fun checkInheritance(wallets: List<AssistedWalletBrief>) = viewModelScope.launch {
        val walletsUnSetupInheritance =
            wallets.filter { it.status == WalletStatus.ACTIVE.name && it.plan.isAllowSetupInheritance() }
        
        if (walletsUnSetupInheritance.isEmpty()) {
            _state.update { it.copy(inheritances = emptyMap()) }
            return@launch
        }
        
        supervisorScope {
            val inheritanceResults = walletsUnSetupInheritance.map { wallet ->
                async {
                    getInheritanceUseCase(
                        GetInheritanceUseCase.Param(
                            walletId = wallet.localId,
                            groupId = wallet.groupId
                        )
                    ).also { result ->
                        if (result.isFailure) {
                            walletsRequestKey = ""
                        }
                    }
                }
            }.awaitAll()
            
            // Check if any call failed and reset key if needed
            val hasFailure = inheritanceResults.any { it.isFailure }
            if (hasFailure) {
                walletsRequestKey = ""
            }
            
            val inheritances = inheritanceResults.mapNotNull { it.getOrNull() }
                .associate { it.walletLocalId to it.status }

            _state.update { it.copy(inheritances = inheritances) }
            mapGroupWalletUi()
        }
    }

    private fun checkMemberMembership() {
        viewModelScope.launch {
            val result = getUserSubscriptionUseCase(Unit)
            if (result.isSuccess) {
                val subscription = result.getOrThrow()
                val getServerWalletResult = getServerWalletsUseCase(Unit)
                if (getServerWalletResult.isFailure) return@launch
                keyPolicyMap.clear()
                keyPolicyMap.putAll(getServerWalletResult.getOrNull()?.keyPolicyMap.orEmpty())
                _state.update { it.copy(plans = subscription.plans) }
                if (getServerWalletResult.isSuccess && getServerWalletResult.getOrThrow().isNeedReload) {
                    retrieveData()
                } else {
                    mapGroupWalletUi()
                }
            } else {
                _state.update { it.copy(plans = emptyList()) }
            }
            if (result.getOrNull()?.plans.isNullOrEmpty()) {
                val bannerResult = getBannerUseCase(Unit)
                _state.update {
                    it.copy(banner = bannerResult.getOrNull())
                }
            }
        }
    }

    fun updateExistingKey(key: WalletsExistingKey) {
        viewModelScope.launch {
            updateExistingKeyUseCase(
                UpdateExistingKeyUseCase.Params(
                    key.signerServer,
                    key.localSigner,
                    false
                )
            )
                .onFailure {
                    _event.emit(ShowErrorEvent(it))
                }
        }
    }

    fun retrieveData() {
        loadWalletJob?.cancel()
        loadWalletJob = viewModelScope.launch {
            if (getState().groupWalletUis.isEmpty()) {
                _state.update { it.copy(isWalletLoading = true) }
            }
            val walletsDeferred = async { getAllWalletsUseCase(Unit) }
            val pendingWalletsDeferred = async { getPendingGroupsSandboxUseCase(Unit) }
            val groupSandboxWalletsDeferred = async { getGroupWalletsUseCase(Unit) }
            val deprecatedGroupWalletsDeferred = async { getDeprecatedGroupWalletsUseCase(Unit) }

            val wallets = walletsDeferred.await().getOrElse { emptyList() }
            val pendingWallets = pendingWalletsDeferred.await().getOrElse { emptyList() }
            val groupSandboxWallets =
                groupSandboxWalletsDeferred.await().getOrElse { emptyList() }.map { it.id }.toSet()
            val deprecatedGroupWalletIds =
                deprecatedGroupWalletsDeferred.await().getOrElse { emptyList() }.toSet()
            if (signInModeHolder.getCurrentMode().isGuestMode()) {
                setHasWalletInGuestModeUseCase(wallets.isNotEmpty())
            }
            _state.update {
                it.copy(
                    pendingGroupSandboxes = pendingWallets.filter { !it.finalized },
                    groupSandboxWalletIds = groupSandboxWallets,
                    deprecatedGroupWalletIds = deprecatedGroupWalletIds,
                    wallets = wallets,
                    isWalletLoading = false
                )
            }

            mapGroupWalletUi()
            getCampaign()
        }
    }

    private suspend fun mapGroupWalletUi() = withContext(ioDispatcher) {
        val results = arrayListOf<GroupWalletUi>()
        val wallets = getState().wallets
        val groups = getState().allGroups
        val assistedWallets = getState().assistedWallets
        val alerts = getState().alerts
        val pendingGroupSandboxes = getState().pendingGroupSandboxes
        val groupSandboxWalletIds = getState().groupSandboxWalletIds
        val walletOrderMap = _walletOrderMap.value
        val pendingGroup = groups.filter { it.isPendingWallet() }
        val isShowPendingPersonalWallet = getState().personalSteps.isNullOrEmpty().not()
        if (isShowPendingPersonalWallet) {
            results.add(GroupWalletUi(isPendingPersonalWallet = true))
        }
        results.addAll(pendingGroupSandboxes.map { GroupWalletUi(sandbox = it) })
        val totalArchivedWallet = wallets.count { it.wallet.archived }
        wallets.filter { !it.wallet.archived }.sortedWith(
            compareBy<WalletExtended>({ walletOrderMap[it.wallet.id]?.order ?: Int.MIN_VALUE })
                .thenByDescending({ it.wallet.createDate })
        ).forEach { wallet ->
            ensureActive()
            val assistedWallet = assistedWallets.find { it.localId == wallet.wallet.id }
            val groupId = assistedWallet?.groupId
            val group = groups.firstOrNull { it.id == groupId }
            val signers = wallet.wallet.signers
                .map { signer -> signer.toModel() }
                .map { signer ->
                    if (signer.type == SignerType.NFC) signer.copy(
                        cardId = cardIdManager.getCardId(
                            signer.id
                        ),
                        internalIndex = signer.internalIndex,
                        externalIndex = signer.externalIndex
                    ) else signer
                }.toList()
            var groupWalletUi = GroupWalletUi(
                wallet = wallet,
                badgeCount = if (alerts[groupId] == null) alerts[wallet.wallet.id].orDefault(0) else alerts[groupId].orDefault(
                    0
                ),
                keyStatus = getState().keyHealthStatus[wallet.wallet.id].orEmpty()
                    .associateBy { it.xfp },
                signers = signers,
                isSandboxWallet = groupSandboxWalletIds.contains(
                    wallet.wallet.id
                )
            )
            if (group != null) {
                val role = byzantineGroupUtils.getCurrentUserRole(group)
                var inviterName = ""
                if ((role == AssistedWalletRole.MASTER.name).not()) {
                    inviterName = byzantineGroupUtils.getInviterName(group)
                }
                groupWalletUi = groupWalletUi.copy(
                    wallet = if (inviterName.isNotEmpty()) null else wallet,
                    group = group,
                    role = role,
                    primaryOwnerMember = byzantineGroupUtils.getPrimaryOwnerMember(
                        group,
                        assistedWallet?.primaryMembershipId
                    ),
                    inviterName = inviterName
                )
            }
            results.add(groupWalletUi)
        }
        pendingGroup.forEach { group ->
            ensureActive()
            var groupWalletUi = GroupWalletUi(group = group)
            val role = byzantineGroupUtils.getCurrentUserRole(group)
            var inviterName = ""
            if ((role == AssistedWalletRole.MASTER.name).not()) {
                inviterName = byzantineGroupUtils.getInviterName(group)
            }
            groupWalletUi = groupWalletUi.copy(
                group = group,
                role = role,
                inviterName = inviterName,
                badgeCount = alerts[group.id] ?: 0
            )
            results.add(groupWalletUi)
        }

        val (groupsWithNullWallet, groupsWithNonNullWallet) = results.partition { it.wallet == null }
        val sortedGroupsWithNullWallet =
            groupsWithNullWallet.sortedByDescending { it.group?.createdTimeMillis }
        val mergedSortedGroups = sortedGroupsWithNullWallet + groupsWithNonNullWallet

        withContext(Main) {
            _state.update {
                it.copy(
                    groupWalletUis = mergedSortedGroups,
                    totalArchivedWallet = totalArchivedWallet,
                    stage = getGroupStage()
                )
            }
        }
    }

    fun updateBadge() {
        viewModelScope.launch {
            val groupIds = getState().allGroups.map { it.id }
            val assistedWalletIdsWithoutGroupId =
                getState().assistedWallets.filter { it.groupId.isEmpty() && it.status != WalletStatus.LOCKED.name }
                    .map { it.localId }
            if (groupIds.isEmpty() && assistedWalletIdsWithoutGroupId.isEmpty()) return@launch
            isRetrievingAlert.set(true)
            val result = getPendingWalletNotifyCountUseCase(
                GetPendingWalletNotifyCountUseCase.Param(
                    groupIds = groupIds,
                    walletIds = assistedWalletIdsWithoutGroupId
                )
            )
            isRetrievingAlert.set(false)
            if (result.isSuccess) {
                val alerts = result.getOrDefault(hashMapOf())
                _state.update { it.copy(alerts = alerts) }
                mapGroupWalletUi()
            }
        }
    }

    fun getKeyHealthStatus() {
        if (isRetrievingKeyHealthStatus.get() || getState().assistedWallets.isEmpty()) return
        viewModelScope.launch {
            isRetrievingKeyHealthStatus.set(true)
            getListGroupWalletKeyHealthStatusUseCase(
                GetListGroupWalletKeyHealthStatusUseCase.Params(
                    getState().assistedWallets.map { it.groupId to it.localId }
                )
            ).onSuccess { keyHealthStatus ->
                isRetrievingKeyHealthStatus.set(false)
                _state.update { it.copy(keyHealthStatus = keyHealthStatus) }
                mapGroupWalletUi()
            }
        }
    }

    fun hasWallet() = getState().wallets.isNotEmpty()

    fun getSatsCardStatus(isoDep: IsoDep?) {
        isoDep ?: return
        viewModelScope.launch {
            _event.emit(NfcLoading(true))
            val result = getNfcCardStatusUseCase(BaseNfcUseCase.Data(isoDep))
            _event.emit(NfcLoading(false))
            if (result.isSuccess) {
                val status = result.getOrThrow()
                if (status is TapSignerStatus) {
                    _event.emit(GetTapSignerStatusSuccess(status))
                } else if (status is SatsCardStatus) {
                    if (status.isUsedUp) {
                        _event.emit(SatsCardUsedUp(status.numberOfSlot))
                    } else if (status.isNeedSetup) {
                        _event.emit(NeedSetupSatsCard(status))
                    } else {
                        _event.emit(GoToSatsCardScreen(status))
                    }
                }
            } else {
                _event.emit(ShowErrorEvent(result.exceptionOrNull()))
            }
        }
    }

    // Don't change, logic is very complicated :D
    fun getGroupStage(): MembershipStage {
        val allGroups = getState().allGroups
        val inheritances = getState().inheritances
        val assistedWallets = getState().assistedWallets
        if (allGroups.isNotEmpty()) {
            return MembershipStage.DONE
        }
        if (assistedWallets.isNotEmpty()) {
            if (assistedWallets.size == 1
                && assistedWallets.first().status == WalletStatus.ACTIVE.name
                && assistedWallets.first().plan == MembershipPlan.HONEY_BADGER
            ) {
                val status = inheritances[assistedWallets.first().localId]
                if (status != null && status == InheritanceStatus.PENDING_CREATION) {
                    return MembershipStage.SETUP_INHERITANCE
                }
            }
            return MembershipStage.DONE
        }
        val plans = getState().plans
        if (!plans.isNullOrEmpty()) {
            if (getState().personalSteps.orEmpty().isNotEmpty()) {
                return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
            }
            return MembershipStage.NONE
        }

        return MembershipStage.DONE
    }

    fun getAssistedWalletId() = getState().assistedWallets.firstOrNull()?.localId

    fun getKeyPolicy(walletId: String) = keyPolicyMap[walletId]

    fun isPremiumUser() = !getState().plans.isNullOrEmpty()

    fun getPlans() = getState().plans

    fun acceptInviteMember(groupId: String, role: String) = viewModelScope.launch {
        _event.emit(Loading(true))
        groupMemberAcceptRequestUseCase(groupId)
            .onSuccess {
                val walletId = getState().assistedWallets.find { it.groupId == groupId }?.localId
                val wallet = getState().wallets.find { it.wallet.id == walletId }
                _event.emit(
                    WalletsEvent.AcceptWalletInvitationSuccess(
                        walletId,
                        groupId,
                        role,
                        wallet == null
                    )
                )
                syncGroup()
            }.onFailure {
                _event.emit(ShowErrorEvent(it))
            }
        _event.emit(Loading(false))
    }

    fun getWalletDetail(walletId: String) = viewModelScope.launch {
        getWalletUseCase.execute(walletId)
            .flowOn(IO)
            .onException {}
            .flowOn(Main)
            .collect {
                checkUserInRoom(it)
            }
    }

    private fun checkUserInRoom(walletExtended: WalletExtended) {
        val roomWallet = walletExtended.roomWallet
        viewModelScope.launch {
            if (roomWallet == null) {
                _event.emit(WalletsEvent.CheckLeaveRoom(false, walletExtended))
                return@launch
            }
            val result = withContext(ioDispatcher) {
                sessionHolder.getSafeActiveSession()?.let {
                    val account = accountManager.getAccount()
                    it.roomService().getRoom(roomWallet.roomId)?.membershipService()
                        ?.getRoomMember(account.chatId)
                }
            }
            _event.emit(
                WalletsEvent.CheckLeaveRoom(
                    result?.membership == Membership.LEAVE,
                    walletExtended
                )
            )
        }
    }

    fun denyInviteMember(groupId: String) = viewModelScope.launch {
        val result = groupMemberDenyRequestUseCase(groupId)
        if (result.isSuccess) {
            retrieveData()
            _event.emit(WalletsEvent.DenyWalletInvitationSuccess)
        } else {
            _event.emit(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    fun getPersonalSteps() = getState().personalSteps.orEmpty()

    fun getWallet(walletId: String) = getState().wallets.find { it.wallet.id == walletId }

    fun getCurrentCampaign(): Campaign? = getState().campaign

    fun getLocalReferrerCode() = getState().localReferrerCode

    fun onMove(fromWalletId: String, toWalletId: String) {
        val updatedList = getState().groupWalletUis.toMutableList()
        val fromIndex = updatedList.indexOfFirst { it.wallet?.wallet?.id == fromWalletId }
        val toIndex = updatedList.indexOfFirst { it.wallet?.wallet?.id == toWalletId }

        if (fromIndex != -1 && toIndex != -1) {
            val temp = updatedList[fromIndex]
            updatedList[fromIndex] = updatedList[toIndex]
            updatedList[toIndex] = temp
            _state.update { it.copy(groupWalletUis = updatedList) }

            val sortedWallets = updatedList.mapNotNull { it.wallet?.wallet?.id }
            insertWalletOrderJob?.cancel()
            insertWalletOrderJob = viewModelScope.launch {
                val orders = sortedWallets.mapIndexed { index, walletId ->
                    WalletOrder(walletId = walletId, order = index)
                }
                insertWalletOrderListUseCase(InsertWalletOrderListUseCase.Params(orders))
            }
        }
    }

    private fun checkWalletsRequestKey(wallets: List<AssistedWalletBrief>, onConsumed: () -> Unit) {
        val key = wallets.joinToString(separator = "|") { "${it.localId}_${it.groupId}" }
        if (walletsRequestKey == key) return
        
        walletsRequestKey = key
        onConsumed()
    }
}