package com.nunchuk.android.main.components.tabs.wallet

import android.nfc.tech.IsoDep
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.*
import com.nunchuk.android.core.domain.membership.GetServerWalletUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.*
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
internal class WalletsViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val getNfcCardStatusUseCase: GetNfcCardStatusUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val accountManager: AccountManager,
    private val getUserSubscriptionUseCase: GetUserSubscriptionUseCase,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    isShowNfcUniversalUseCase: IsShowNfcUniversalUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    private val tapSignerCardIds = hashMapOf<String, String>()
    private val keyPolicyMap = hashMapOf<String, KeyPolicy>()

    val isShownNfcUniversal = isShowNfcUniversalUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var isRetrievingData = AtomicBoolean(false)

    override val initialState = WalletsState()

    init {
        checkMemberMembership()

        viewModelScope.launch {
            assistedWalletManager.assistedWalletIds.collect {
                updateState { copy(assistedWalletIds = it) }
            }
        }
    }

    private fun checkMemberMembership() {
        viewModelScope.launch {
            val result = getUserSubscriptionUseCase(Unit)
            if (result.isSuccess) {
                val subscription = result.getOrThrow().copy(slug = HONEY_BADGER_PLAN) // TODO Hai remove
                val isPremiumUser = subscription.subscriptionId.isNullOrEmpty().not()
                val getServerWalletResult = getServerWalletUseCase(Unit)
                if (getServerWalletResult.isSuccess && getServerWalletResult.getOrThrow().isNeedReload) {
                    retrieveData()
                }
                keyPolicyMap.clear()
                keyPolicyMap.putAll(getServerWalletResult.getOrNull()?.keyPolicyMap.orEmpty())
                if (subscription.slug == IRON_HAND_PLAN) {
                    membershipStepManager.setCurrentPlan(MembershipPlan.IRON_HAND)
                } else if (subscription.slug == HONEY_BADGER_PLAN) {
                    membershipStepManager.setCurrentPlan(MembershipPlan.HONEY_BADGER)
                }
                updateState {
                    copy(
                        isPremiumUser = isPremiumUser,
                        hasCreatedWallet = getServerWalletResult.getOrThrow().planWalletCreated.contains(subscription.slug),
                    )
                }
            } else {
                updateState {
                    copy(
                        isPremiumUser = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            membershipStepManager.remainingTime.collect {
                updateState { copy(remainingTime = it) }
            }
        }
    }

    fun getAppSettings() {
        viewModelScope.launch {
            getAppSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(chain = it.chain)
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
                    p.first.asSequence().filter { it.type == SignerType.NFC }.forEach {
                        if (tapSignerCardIds.contains(it.id).not()) {
                            tapSignerCardIds[it.id] =
                                getTapSignerStatusByIdUseCase(it.id).getOrNull()?.ident.orEmpty()
                        }
                    }
                    Triple(p.first, p.second, wallets)
                }
                .flowOn(Dispatchers.IO)
                .onException {
                    updateState { copy(signers = emptyList(), masterSigners = emptyList()) }
                }
                .flowOn(Dispatchers.Main)
                .onCompletion {
                    event(Loading(false))
                    isRetrievingData.set(false)
                }
                .collect {
                    val (masterSigners, signers, wallets) = it
                    val newMasterSigner =
                        masterSigners.sortedByDescending { signer -> isPrimaryKey(signer.id) }
                    updateState {
                        copy(
                            masterSigners = newMasterSigner,
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

    fun hasSigner() = getState().signers.isNotEmpty() || getState().masterSigners.isNotEmpty()

    fun hasWallet() = getState().wallets.isNotEmpty()

    fun isPremiumUser() = getState().isPremiumUser

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

    fun mapSigners(): List<SignerModel> {
        val state = getState()
        return state.masterSigners.map {
            masterSignerMapper(
                it,
                cardId = tapSignerCardIds[it.id].orEmpty()
            )
        } + state.signers.map(SingleSigner::toModel)
    }

    fun getGroupStage(): MembershipStage = when {
        membershipStepManager.isNotConfig() -> MembershipStage.NONE
        membershipStepManager.isCreatedAssistedWalletDone() -> MembershipStage.DONE
        membershipStepManager.isConfigKeyDone() -> MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
        else -> MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }

    fun getKeyPolicy(walletId: String) = keyPolicyMap[walletId]

    companion object {
        private const val IRON_HAND_PLAN = "iron_hand"
        private const val HONEY_BADGER_PLAN = "honey_badger"
    }
}