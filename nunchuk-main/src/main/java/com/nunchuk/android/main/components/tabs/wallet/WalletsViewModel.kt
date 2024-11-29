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
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetNfcCardStatusUseCase
import com.nunchuk.android.core.domain.GetRemotePriceConvertBTCUseCase
import com.nunchuk.android.core.domain.IsShowNfcUniversalUseCase
import com.nunchuk.android.core.domain.membership.CheckWalletsExistingKeyUseCase
import com.nunchuk.android.core.domain.membership.GetServerWalletsUseCase
import com.nunchuk.android.core.domain.membership.UpdateExistingKeyUseCase
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.AddWalletEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GetTapSignerStatusSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GoToSatsCardScreen
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.Loading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NeedSetupSatsCard
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NfcLoading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.None
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.SatsCardUsedUp
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowErrorEvent
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.containsPersonalPlan
import com.nunchuk.android.model.isAllowSetupInheritance
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.setting.HomeDisplaySetting
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetHomeDisplaySettingUseCase
import com.nunchuk.android.usecase.GetLocalCurrencyUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.MigrateHomeDisplaySettingUseCase
import com.nunchuk.android.usecase.UpdateHomeDisplaySettingUseCase
import com.nunchuk.android.usecase.banner.GetBannerUseCase
import com.nunchuk.android.usecase.byzantine.GetListGroupWalletKeyHealthStatusUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberAcceptRequestUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberDenyRequestUseCase
import com.nunchuk.android.usecase.byzantine.SyncDeletedWalletUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.campaign.GetCurrentCampaignUseCase
import com.nunchuk.android.usecase.campaign.GetLocalCurrentCampaignUseCase
import com.nunchuk.android.usecase.campaign.GetLocalReferrerCodeUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.GetPendingWalletNotifyCountUseCase
import com.nunchuk.android.usecase.membership.GetPersonalMembershipStepUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.user.IsHideUpsellBannerUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.room.model.Membership
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
internal class WalletsViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val getNfcCardStatusUseCase: GetNfcCardStatusUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val masterSignerMapper: MasterSignerMapper,
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<WalletsState, WalletsEvent>() {
    private val keyPolicyMap = hashMapOf<String, KeyPolicy>()

    val isShownNfcUniversal = isShowNfcUniversalUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val isHideUpsellBanner = isHideUpsellBannerUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var isRetrievingData = AtomicBoolean(false)
    private var isRetrievingAlert = AtomicBoolean(false)
    private var isRetrievingKeyHealthStatus = AtomicBoolean(false)

    private var walletsRequestKey = ""

    override val initialState = WalletsState()

    init {
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect {
                    updateState { copy(assistedWallets = it) }
                    mapGroupWalletUi()
                    checkWalletsRequestKey(it) {
                        checkInheritance(it)
                        getKeyHealthStatus()
                    }
                }
        }
        checkMemberMembership()
        viewModelScope.launch {
            membershipStepManager.remainingTime.collect {
                updateState { copy(remainingTime = it) }
            }
        }
        viewModelScope.launch {
            isHideUpsellBanner.collect {
                updateState { copy(isHideUpsellBanner = it) }
            }
        }
        viewModelScope.launch {
            getLocalCurrencyUseCase(Unit).distinctUntilChanged().collect {
                LOCAL_CURRENCY = it.getOrDefault(USD_CURRENCY)
                getRemotePriceConvertBTCUseCase(Unit)
            }
        }
        viewModelScope.launch {
            getPersonalMembershipStepUseCase(Unit).map {
                it.getOrElse { emptyList() }
            }.distinctUntilChanged().collect {
                updateState { copy(personalSteps = it) }
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
                        val assistedWallets =
                            getState().assistedWallets.filter { it.localId == event.walletId }
                        if (assistedWallets.isNotEmpty()) {
                            checkInheritance(assistedWallets)
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
                updateState { copy(allGroups = groups) }
                updateBadge()
                getKeyHealthStatus()
                mapGroupWalletUi()
            }
        }
        getAppSettings()
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect {
                    updateState {
                        copy(
                            walletSecuritySetting = it.getOrNull() ?: WalletSecuritySetting()
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
            state.asFlow().map { it.plans.orEmpty().containsPersonalPlan() }
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
                .collect {
                    updateState {
                        copy(
                            homeDisplaySetting = it.getOrNull() ?: HomeDisplaySetting()
                        )
                    }
                }
        }
    }

    private var mapDataJob: Job? = null

    private fun getCampaign() {
        viewModelScope.launch {
            getLocalCurrentCampaignUseCase(Unit).collect {
                updateState { copy(campaign = it.getOrNull()) }
            }
        }
    }

    private fun getReferrerCode() {
        viewModelScope.launch {
            getLocalReferrerCodeUseCase(Unit).distinctUntilChanged().collect {
                updateState { copy(localReferrerCode = it.getOrNull()) }
                getCurrentCampaignUseCase(GetCurrentCampaignUseCase.Param(accountManager.getAccount().email.ifEmpty { it.getOrNull()?.email }))
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
        val walletsUnSetupInheritance = wallets.filter { it.plan.isAllowSetupInheritance() }
        supervisorScope {
            walletsUnSetupInheritance.map {
                async {
                    getInheritanceUseCase(
                        GetInheritanceUseCase.Param(
                            it.localId,
                            groupId = it.groupId
                        )
                    )
                }
            }.awaitAll()
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
                updateState { copy(plans = subscription.plans) }
                if (getServerWalletResult.isSuccess && getServerWalletResult.getOrThrow().isNeedReload) {
                    retrieveData()
                } else {
                    mapGroupWalletUi()
                }
            } else {
                updateState { copy(plans = emptyList()) }
            }
            if (result.getOrNull()?.plans.isNullOrEmpty()) {
                val bannerResult = getBannerUseCase(Unit)
                updateState {
                    copy(banner = bannerResult.getOrNull())
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
                    setEvent(ShowErrorEvent(it))
                }
        }
    }

    private fun getAppSettings() {
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit).map { it.getOrElse { Chain.MAIN } }.collect {
                updateState {
                    copy(chain = it)
                }
            }
        }
    }

    fun retrieveData() {
        if (isRetrievingData.get()) return
        isRetrievingData.set(true)
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException {
                    Timber.e(it)
                }.flowOn(Dispatchers.Main)
                .onStart {
                    if (getState().wallets.isEmpty()) {
                        event(Loading(true))
                    }
                }.onCompletion {
                    event(Loading(false))
                    isRetrievingData.set(false)
                }.collect {
                    updateState { copy(wallets = it) }
                    mapGroupWalletUi()
                    getCampaign()
                }
        }
    }

    private fun mapGroupWalletUi() {
        mapDataJob?.cancel()
        mapDataJob = viewModelScope.launch(ioDispatcher) {
            val results = arrayListOf<GroupWalletUi>()
            val wallets = getState().wallets
            val groups = getState().allGroups
            val assistedWallets = getState().assistedWallets
            val alerts = getState().alerts
            val pendingGroup = groups.filter { it.isPendingWallet() }
            val isShowPendingPersonalWallet = getState().personalSteps.isNullOrEmpty().not()
            if (isShowPendingPersonalWallet) {
                results.add(GroupWalletUi(isPendingPersonalWallet = true))
            }
            wallets.forEach { wallet ->
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
                            )
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
            val sortedGroupsWithNonNullWallet =
                groupsWithNonNullWallet.sortedByDescending { it.wallet?.wallet?.createDate }
            val mergedSortedGroups = sortedGroupsWithNullWallet + sortedGroupsWithNonNullWallet

            withContext(Dispatchers.Main) {
                updateState { copy(groupWalletUis = mergedSortedGroups) }
            }
        }
    }

    fun updateBadge() {
        if (isRetrievingAlert.get()) return
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
                updateState { copy(alerts = alerts) }
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
            ).onSuccess {
                isRetrievingKeyHealthStatus.set(false)
                updateState { copy(keyHealthStatus = it) }
                mapGroupWalletUi()
            }
        }
    }

    fun handleAddWallet() {
        event(AddWalletEvent)
    }

    fun hasWallet() = getState().wallets.isNotEmpty()

    fun getSatsCardStatus(isoDep: IsoDep?) {
        isoDep ?: return
        viewModelScope.launch {
            setEvent(NfcLoading(true))
            val result = getNfcCardStatusUseCase(BaseNfcUseCase.Data(isoDep))
            setEvent(NfcLoading(false))
            if (result.isSuccess) {
                val status = result.getOrThrow()
                if (status is TapSignerStatus) {
                    setEvent(GetTapSignerStatusSuccess(status))
                } else if (status is SatsCardStatus) {
                    if (status.isUsedUp) {
                        setEvent(SatsCardUsedUp(status.numberOfSlot))
                    } else if (status.isNeedSetup) {
                        setEvent(NeedSetupSatsCard(status))
                    } else {
                        setEvent(GoToSatsCardScreen(status))
                    }
                }
            } else {
                setEvent(ShowErrorEvent(result.exceptionOrNull()))
            }
        }
    }

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>, masterSigners: List<MasterSigner>,
    ): List<SignerModel> {
        return masterSigners.map {
            masterSignerMapper(it)
        } + singleSigners.map(SingleSigner::toModel)
    }

    // Don't change, logic is very complicated :D
    fun getGroupStage(): MembershipStage {
        val allGroups = getState().allGroups
        val assistedWallets = getState().assistedWallets
        if (allGroups.isNotEmpty()) {
            return MembershipStage.DONE
        }
        if (assistedWallets.isNotEmpty()) {
            if (assistedWallets.size == 1
                && !assistedWallets.first().isSetupInheritance
                && assistedWallets.first().status == WalletStatus.ACTIVE.name
                && assistedWallets.first().plan == MembershipPlan.HONEY_BADGER
            ) {
                return MembershipStage.SETUP_INHERITANCE
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

    fun clearEvent() = event(None)

    fun acceptInviteMember(groupId: String, role: String) = viewModelScope.launch {
        setEvent(Loading(true))
        groupMemberAcceptRequestUseCase(groupId)
            .onSuccess {
                val walletId = getState().assistedWallets.find { it.groupId == groupId }?.localId
                val wallet = getState().wallets.find { it.wallet.id == walletId }
                setEvent(
                    WalletsEvent.AcceptWalletInvitationSuccess(
                        walletId,
                        groupId,
                        role,
                        wallet == null
                    )
                )
                syncGroup()
            }.onFailure {
                event(ShowErrorEvent(it))
            }
        setEvent(Loading(false))
    }

    fun checkUserInRoom(walletExtended: WalletExtended) {
        val roomWallet = walletExtended.roomWallet
        if (roomWallet == null) {
            setEvent(WalletsEvent.CheckLeaveRoom(false, walletExtended))
            return
        }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                sessionHolder.getSafeActiveSession()?.let {
                    val account = accountManager.getAccount()
                    it.roomService().getRoom(roomWallet.roomId)?.membershipService()
                        ?.getRoomMember(account.chatId)
                }
            }
            setEvent(WalletsEvent.CheckLeaveRoom(result?.membership == Membership.LEAVE, walletExtended))
        }
    }

    fun denyInviteMember(groupId: String) = viewModelScope.launch {
        val result = groupMemberDenyRequestUseCase(groupId)
        if (result.isSuccess) {
            retrieveData()
            event(WalletsEvent.DenyWalletInvitationSuccess)
        } else {
            event(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    fun getPersonalSteps() = getState().personalSteps.orEmpty()

    fun getWallet(walletId: String) = getState().wallets.find { it.wallet.id == walletId }

    fun getCurrentCampaign(): Campaign? = getState().campaign

    fun getLocalReferrerCode() = getState().localReferrerCode

    fun getBanner() = getState().banner

    private fun checkWalletsRequestKey(wallets: List<AssistedWalletBrief>, onConsumed: () -> Unit) {
        val key = wallets.joinToString { "${it.localId}_${it.groupId}" }
        if (walletsRequestKey == key) return
        walletsRequestKey = key
        onConsumed()
    }
}