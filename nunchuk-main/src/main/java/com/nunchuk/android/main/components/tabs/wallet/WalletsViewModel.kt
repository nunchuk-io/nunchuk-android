/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetNfcCardStatusUseCase
import com.nunchuk.android.core.domain.IsShowNfcUniversalUseCase
import com.nunchuk.android.core.domain.membership.GetServerWalletUseCase
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.*
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.banner.GetBannerUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import com.nunchuk.android.usecase.user.IsHideUpsellBannerUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val getInheritanceUseCase: GetInheritanceUseCase,
    private val getBannerUseCase: GetBannerUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    isShowNfcUniversalUseCase: IsShowNfcUniversalUseCase,
    isHideUpsellBannerUseCase: IsHideUpsellBannerUseCase,
) : NunchukViewModel<WalletsState, WalletsEvent>() {
    private val keyPolicyMap = hashMapOf<String, KeyPolicy>()

    val isShownNfcUniversal = isShowNfcUniversalUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val isHideUpsellBanner = isHideUpsellBannerUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var isRetrievingData = AtomicBoolean(false)

    override val initialState = WalletsState()

    init {
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect {
                    checkMemberMembership()
                    checkInheritance(it)
                    updateState { copy(assistedWallets = it) }
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
    }

    fun reloadMembership() {
        viewModelScope.launch {
            delay(1000L)
            checkMemberMembership()
        }
    }

    private suspend fun checkInheritance(wallets: List<AssistedWalletBrief>) {
        val honeyWalletsUnSetupInheritance =
            wallets.filter { it.plan == MembershipPlan.HONEY_BADGER && it.isSetupInheritance.not() }
        supervisorScope {
            honeyWalletsUnSetupInheritance.map {
                async {
                    getInheritanceUseCase(it.localId)
                }
            }.awaitAll()
        }
    }

    private fun checkMemberMembership() {
        viewModelScope.launch {
            val result = getUserSubscriptionUseCase(Unit)
            if (result.isSuccess) {
                val subscription = result.getOrThrow()
                val isPremiumUser = subscription.subscriptionId.isNullOrEmpty()
                    .not() && subscription.plan != MembershipPlan.NONE
                val getServerWalletResult = getServerWalletUseCase(Unit)
                if (getServerWalletResult.isFailure) return@launch
                if (getServerWalletResult.isSuccess && getServerWalletResult.getOrThrow().isNeedReload) {
                    retrieveData()
                }
                keyPolicyMap.clear()
                keyPolicyMap.putAll(getServerWalletResult.getOrNull()?.keyPolicyMap.orEmpty())
                updateState { copy(isPremiumUser = isPremiumUser) }
            } else {
                updateState { copy(isPremiumUser = false) }
            }
            if (result.getOrNull()?.subscriptionId.isNullOrEmpty() && isHideUpsellBanner.value.not()) {
                val bannerResult = getBannerUseCase(Unit)
                updateState {
                    copy(banner = bannerResult.getOrNull())
                }
            }
        }
    }

    fun getAppSettings() {
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrElse { Chain.MAIN } }
                .collect {
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
            getCompoundSignersUseCase.execute()
                .zip(getWalletsUseCase.execute()) { p, wallets ->
                    Triple(p.first, p.second, wallets)
                }
                .map {
                    mapSigners(it.second, it.first).sortedByDescending { signer ->
                        isPrimaryKey(
                            signer.id
                        )
                    } to it.third
                }
                .flowOn(Dispatchers.IO)
                .onException {
                    updateState { copy(signers = emptyList()) }
                }
                .flowOn(Dispatchers.Main)
                .onCompletion {
                    event(Loading(false))
                    isRetrievingData.set(false)
                }
                .collect {
                    val (signers, wallets) = it
                    updateState {
                        copy(
                            signers = signers,
                            wallets = wallets
                        )
                    }
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

    fun isInWallet(signer: SignerModel): Boolean {
        return getState().wallets
            .any {
                it.wallet.signers.any anyLast@{ singleSigner ->
                    if (singleSigner.hasMasterSigner) {
                        return@anyLast singleSigner.masterFingerprint == signer.fingerPrint
                    }
                    return@anyLast singleSigner.masterFingerprint == signer.fingerPrint
                            && singleSigner.derivationPath == signer.derivationPath
                }
            }
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
        singleSigners: List<SingleSigner>,
        masterSigners: List<MasterSigner>
    ): List<SignerModel> {
        return masterSigners.map {
            masterSignerMapper(it)
        } + singleSigners.map(SingleSigner::toModel)
    }

    fun getGroupStage(): MembershipStage {
        if (getState().assistedWallets.all { it.isSetupInheritance || it.plan == MembershipPlan.IRON_HAND }) return MembershipStage.DONE
        if (getState().assistedWallets.any { it.isSetupInheritance && it.plan == MembershipPlan.HONEY_BADGER }) return MembershipStage.SETUP_INHERITANCE
        if (membershipStepManager.isNotConfig()) return MembershipStage.NONE
        return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }

    fun getKeyPolicy(walletId: String) = keyPolicyMap[walletId]

    fun isPremiumUser() = getState().isPremiumUser == true
}