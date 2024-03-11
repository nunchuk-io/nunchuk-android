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
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetNfcCardStatusUseCase
import com.nunchuk.android.core.domain.GetRemotePriceConvertBTCUseCase
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.domain.IsShowNfcUniversalUseCase
import com.nunchuk.android.core.domain.membership.GetServerWalletsUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.mapper.MasterSignerMapper
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
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.CheckWalletPin
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GetTapSignerStatusSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GoToSatsCardScreen
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.Loading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NeedSetupSatsCard
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NfcLoading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.None
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.SatsCardUsedUp
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowErrorEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowSignerIntroEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.VerifyPassphraseSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.VerifyPasswordSuccess
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetLocalCurrencyUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.banner.GetBannerUseCase
import com.nunchuk.android.usecase.byzantine.GetListGroupWalletKeyHealthStatusUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberAcceptRequestUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberDenyRequestUseCase
import com.nunchuk.android.usecase.byzantine.SyncDeletedWalletUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.membership.GetAssistedWalletConfigUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.GetPendingWalletNotifyCountUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import com.nunchuk.android.usecase.user.IsHideUpsellBannerUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
internal class WalletsViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
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
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase,
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val getAssistedWalletConfigUseCase: GetAssistedWalletConfigUseCase,
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

    override val initialState = WalletsState()

    init {
        viewModelScope.launch {
            getAssistedWalletConfigUseCase(Unit)
        }
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect {
                    updateState { copy(assistedWallets = it) }
                    checkMemberMembership()
                    checkInheritance(it)
                    mapGroupWalletUi()
                    getKeyHealthStatus()
                }
        }
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
            getWalletPinUseCase(Unit).collect {
                updateState { copy(currentWalletPin = it.getOrDefault("")) }
            }
        }
        viewModelScope.launch {
            getLocalCurrencyUseCase(Unit).collect {
                LOCAL_CURRENCY = it.getOrDefault(USD_CURRENCY)
                getRemotePriceConvertBTCUseCase(Unit)
            }
        }
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                when (event) {
                    is PushEvent.WalletCreate -> {
                        if (!getState().wallets.any { it.wallet.id == event.walletId }) {
                            getServerWalletsUseCase(Unit).onSuccess {
                                if (it.isNeedReload) {
                                    retrieveData()
                                }
                            }
                        }
                    }

                    is PushEvent.DraftResetWallet -> {
                        syncGroupWalletsUseCase(Unit).onSuccess { shouldReload ->
                            if (shouldReload) retrieveData()
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
                        val assistedWallets = getState().assistedWallets.filter { it.localId == event.walletId}
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
            if (accountManager.getAccount().id.isEmpty()) {
                getUserProfileUseCase(Unit)
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

    private suspend fun checkInheritance(wallets: List<AssistedWalletBrief>) {
        val walletsUnSetupInheritance =
            wallets.filter { it.plan == MembershipPlan.HONEY_BADGER || it.plan == MembershipPlan.BYZANTINE_PRO }
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
                updateState { copy(plan = subscription.plan) }
                if (getServerWalletResult.isSuccess && getServerWalletResult.getOrThrow().isNeedReload) {
                    retrieveData()
                } else {
                    mapGroupWalletUi()
                }
            } else {
                updateState { copy(plan = MembershipPlan.NONE) }
            }
            if (result.getOrNull()?.subscriptionId.isNullOrEmpty() && isHideUpsellBanner.value.not()) {
                val bannerResult = getBannerUseCase(Unit)
                updateState {
                    copy(banner = bannerResult.getOrNull())
                }
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
            getCompoundSignersUseCase.execute().zip(getWalletsUseCase.execute()) { p, wallets ->
                Triple(p.first, p.second, wallets)
            }.map {
                mapSigners(it.second, it.first).sortedByDescending { signer ->
                    isPrimaryKey(
                        signer.id
                    )
                } to it.third
            }.flowOn(Dispatchers.IO).onException {
                updateState { copy(signers = emptyList()) }
            }.flowOn(Dispatchers.Main).onStart {
                if (getState().wallets.isEmpty()) {
                    event(Loading(true))
                }
            }.onCompletion {
                event(Loading(false))
                isRetrievingData.set(false)
            }.collect {
                val (signers, wallets) = it
                updateState {
                    copy(
                        signers = signers, wallets = wallets
                    )
                }
                mapGroupWalletUi()
            }
        }
    }

    private fun mapGroupWalletUi() {
        viewModelScope.launch(ioDispatcher) {
            val results = arrayListOf<GroupWalletUi>()
            val wallets = getState().wallets
            val groups = getState().allGroups
            val assistedWallets = getState().assistedWallets
            val alerts = getState().alerts
            val assistedWalletIds = assistedWallets.map { it.localId }.toHashSet()
            val pendingGroup = groups.filter { it.isPendingWallet() }
            wallets.forEach { wallet ->
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
                    isAssistedWallet = wallet.wallet.id in assistedWalletIds,
                    badgeCount = if (alerts[groupId] == null) alerts[wallet.wallet.id].orDefault(0) else alerts[groupId].orDefault(0),
                    keyStatus = getState().keyHealthStatus[wallet.wallet.id].orEmpty().associateBy { it.xfp },
                    signers = signers
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
                        primaryOwnerMember = byzantineGroupUtils.getPrimaryOwnerMember(group, assistedWallet?.primaryMembershipId),
                        inviterName = inviterName
                    )
                }
                results.add(groupWalletUi)
            }
            pendingGroup.forEach { group ->
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
            val assistedWalletIdsWithoutGroupId = getState().assistedWallets.filter { it.groupId.isEmpty() }.map { it.localId }
            if (groupIds.isEmpty() && assistedWalletIdsWithoutGroupId.isEmpty()) return@launch
            isRetrievingAlert.set(true)
            val result = getPendingWalletNotifyCountUseCase(
                GetPendingWalletNotifyCountUseCase.Param(
                    groupIds = groupIds,
                    walletIds = assistedWalletIdsWithoutGroupId
                ))
            isRetrievingAlert.set(false)
            if (result.isSuccess) {
                val alerts = result.getOrDefault(hashMapOf())
                updateState { copy(alerts = alerts) }
                mapGroupWalletUi()
            }
        }
    }

    fun getKeyHealthStatus() {
        if (isRetrievingKeyHealthStatus.get()) return
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

    private fun isPrimaryKey(id: String) =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && accountManager.getPrimaryKeyInfo()?.xfp == id

    fun handleAddSigner() {
        event(ShowSignerIntroEvent)
    }

    fun handleAddWallet() {
        event(AddWalletEvent)
    }

    private fun checkSignerExistence(signer: SignerModel, walletExtendeds: List<WalletExtended>): Boolean {
        return walletExtendeds.any {
            it.wallet.signers.any anyLast@{ singleSigner ->
                if (singleSigner.hasMasterSigner) {
                    return@anyLast singleSigner.masterFingerprint == signer.fingerPrint
                }
                return@anyLast singleSigner.masterFingerprint == signer.fingerPrint && singleSigner.derivationPath == signer.derivationPath
            }
        }
    }

    fun isInWallet(signer: SignerModel): Boolean {
        return checkSignerExistence(signer, getState().wallets)
    }

    fun isInAssistedWallet(signer: SignerModel): Boolean {
        val assistedWalletSet = getState().assistedWallets.map { it.localId }.toHashSet()
        val assistedWallets = getState().wallets.filter { it.wallet.id in assistedWalletSet }
        return checkSignerExistence(signer, assistedWallets)
    }


    fun hasSigner() = getState().signers.isNotEmpty()

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
        val plan = getState().plan
        val assistedWallets = getState().assistedWallets
        when (plan) {
            MembershipPlan.IRON_HAND -> {
                return when {
                    assistedWallets.isNotEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.DONE
                    membershipStepManager.isNotConfig() -> MembershipStage.NONE
                    else -> MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
                }
            }

            MembershipPlan.HONEY_BADGER -> {
                return when {
                    assistedWallets.all { it.isSetupInheritance } && assistedWallets.isNotEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.DONE
                    // Only show setup Inheritance banner with first assisted wallet
                    assistedWallets.isNotEmpty() && assistedWallets.first().isSetupInheritance.not() && membershipStepManager.isNotConfig() -> MembershipStage.SETUP_INHERITANCE
                    assistedWallets.isEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.NONE
                    assistedWallets.isNotEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.DONE
                    else -> MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
                }
            }

            MembershipPlan.BYZANTINE, MembershipPlan.BYZANTINE_PRO, MembershipPlan.BYZANTINE_PREMIER -> {
                val allGroups = getState().allGroups
                return if (allGroups.isEmpty()) MembershipStage.NONE else MembershipStage.DONE
            }

            else -> return MembershipStage.DONE // no subscription plan it means done
        }
    }

    fun getAssistedWalletId() = getState().assistedWallets.firstOrNull()?.localId

    fun getKeyPolicy(walletId: String) = keyPolicyMap[walletId]

    fun isPremiumUser() = getState().plan != null && getState().plan != MembershipPlan.NONE

    fun clearEvent() = event(None)

    fun isWalletPinEnable() =
        getState().walletSecuritySetting.protectWalletPin

    fun isWalletPasswordEnable() =
        accountManager.loginType() == SignInMode.EMAIL.value && getState().walletSecuritySetting.protectWalletPassword

    fun isWalletPassphraseEnable() =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && getState().walletSecuritySetting.protectWalletPassphrase

    fun checkWalletPin(input: String, walletId: String) = viewModelScope.launch {
        val match = checkWalletPinUseCase(input).getOrDefault(false)
        event(CheckWalletPin(match, walletId))
    }

    fun confirmPassword(password: String, walletId: String) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                password = password, targetAction = TargetAction.PROTECT_WALLET.name
            )
        )
        if (result.isSuccess) {
            event(VerifyPasswordSuccess(walletId))
        } else {
            event(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    fun confirmPassphrase(passphrase: String, walletId: String) = viewModelScope.launch {
        if (passphrase.isBlank()) {
            return@launch
        }
        val result = verifiedPKeyTokenUseCase(passphrase)
        if (result.isSuccess) {
            event(VerifyPassphraseSuccess(walletId))
        } else {
            event(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    fun acceptInviteMember(groupId: String) = viewModelScope.launch {
        setEvent(Loading(true))
        groupMemberAcceptRequestUseCase(groupId)
            .onSuccess {
                val walletId = getState().assistedWallets.find { it.groupId == groupId }?.localId
                setEvent(WalletsEvent.AcceptWalletInvitationSuccess(walletId, groupId))
                syncGroup()
            }.onFailure {
                event(ShowErrorEvent(it))
            }
        setEvent(Loading(false))
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
}