package com.nunchuk.android.main.components.tabs.wallet

import android.nfc.tech.IsoDep
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetNfcCardStatusUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
internal class WalletsViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val getNfcCardStatusUseCase: GetNfcCardStatusUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val accountManager: AccountManager
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    private var isRetrievingData = AtomicBoolean(false)

    override val initialState = WalletsState()

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

    fun handleAddSignerOrWallet() {
        if (hasSigner()) {
            handleAddWallet()
        } else {
            handleAddSigner()
        }
    }

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
        return state.masterSigners.map(masterSignerMapper::invoke) + state.signers.map(SingleSigner::toModel)
    }
}