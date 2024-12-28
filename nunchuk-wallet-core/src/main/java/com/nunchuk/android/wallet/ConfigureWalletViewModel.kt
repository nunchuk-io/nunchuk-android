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

package com.nunchuk.android.wallet

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.domain.CacheDefaultTapsignerMasterSignerXPubUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWallets2UseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigureWalletViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getUnusedSignerFromMasterSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
    private val savedStateHandle: SavedStateHandle,
    private val cacheDefaultTapsignerMasterSignerXPubUseCase: CacheDefaultTapsignerMasterSignerXPubUseCase,
    private val getWallets2UseCase: GetWallets2UseCase,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
) : ViewModel() {

    private lateinit var args: ConfigureWalletArgs
    private val masterSignerSingleMap = hashMapOf<String, SingleSigner>()
    private val masterSignerMutisigMap = hashMapOf<String, SingleSigner>()

    private val _state = MutableStateFlow(ConfigureWalletState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ConfigureWalletEvent>()
    val event = _event.asSharedFlow()

    private val masterSignerIdSet = mutableSetOf<String>()
    private val unBackedUpSignerXfpSet = mutableSetOf<String>()

    init {
        getUnBackedUpWallet()
        viewModelScope.launch {
            state.map { it.selectedSigners.size }
                .distinctUntilChanged()
                .collect {
                    _state.update {
                        it.copy(
                            allSigners = mapSigners(it.masterSigners, it.remoteSigners)
                        )
                    }
                }
        }
    }

    private fun getState() = _state.value

    fun init(args: ConfigureWalletArgs) {
        this.args = args
        if (args.addressType.isTaproot()) {
            getSupportedSigner()
        }
        getSigners()
    }

    private fun getSupportedSigner() {
        viewModelScope.launch {
            getSupportedSignersUseCase(Unit).onSuccess { supportedSigners ->
                _state.update { it.copy(supportedSigners = supportedSigners) }
            }
        }
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute()
            .onException {}
            .map { signerPair ->
                masterSignerIdSet.addAll(signerPair.first.map { it.id })
                val singleSigSigners = getUnusedSignerFromMasterSignerUseCase.execute(
                    signerPair.first, WalletType.SINGLE_SIG, args.addressType
                ).first()
                val multiSigSigners = getUnusedSignerFromMasterSignerUseCase.execute(
                    signerPair.first, WalletType.MULTI_SIG, args.addressType
                ).first()
                masterSignerSingleMap.apply {
                    clear()
                    putAll(singleSigSigners.associateBy { it.masterSignerId })
                }
                masterSignerMutisigMap.apply {
                    clear()
                    putAll(multiSigSigners.associateBy { it.masterSignerId })
                }
                _state.update {
                    it.copy(
                        masterSigners = signerPair.first,
                        remoteSigners = signerPair.second,
                        allSigners = mapSigners(signerPair.first, signerPair.second),
                    )
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart { _event.emit(ConfigureWalletEvent.Loading(true)) }
            .onCompletion { _event.emit(ConfigureWalletEvent.Loading(false)) }
            .launchIn(viewModelScope)
    }

    fun cacheTapSignerXpub(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            _event.emit(ConfigureWalletEvent.NfcLoading(true))
            val signer: SignerModel =
                savedStateHandle[EXTRA_CURRENT_SELECTED_MASTER_SIGNER] ?: return@launch
            val result = cacheDefaultTapsignerMasterSignerXPubUseCase(
                CacheDefaultTapsignerMasterSignerXPubUseCase.Data(isoDep, cvc, signer.id)
            )
            _event.emit(ConfigureWalletEvent.NfcLoading(false))
            if (result.isSuccess) {
                updateStateSelectedSigner(true, signer)
                getSigners()
            } else {
                updateStateSelectedSigner(false, signer)
                _event.emit(ConfigureWalletEvent.CacheTapSignerXpubError(result.exceptionOrNull()))
            }
        }
    }

    fun updateSelectedSigner(signer: SignerModel, checked: Boolean) = viewModelScope.launch {
        val masterSigner =
            getState().masterSigners.find { it.device.masterFingerprint == signer.fingerPrint }
        savedStateHandle[EXTRA_CURRENT_SELECTED_MASTER_SIGNER] = signer
        val device = masterSigner?.device
        val isShouldCacheXpub = signer.type == SignerType.NFC
                && !(masterSignerSingleMap.contains(signer.id) && masterSignerMutisigMap.contains(
            signer.id
        ))
        if (checked && isShouldCacheXpub) {
            _event.emit(ConfigureWalletEvent.RequestCacheTapSignerXpub(signer))
        } else if (checked && device?.needPassPhraseSent == true) {
            _event.emit(ConfigureWalletEvent.PromptInputPassphrase(signer))
        } else {
            updateStateSelectedSigner(checked, signer)
        }
    }

    fun verifyPassphrase(signer: SignerModel, passphrase: String) {
        viewModelScope.launch {
            sendSignerPassphrase.execute(signer.id, passphrase).onException {
                _event.emit(ConfigureWalletEvent.ShowError(it.message.orEmpty()))
                updateStateSelectedSigner(false, signer)
            }.collect { updateStateSelectedSigner(true, signer) }
        }
    }

    fun cancelVerifyPassphrase(signer: SignerModel) {
        updateStateSelectedSigner(false, signer)
    }

    private fun updateStateSelectedSigner(checked: Boolean, signer: SignerModel) {
        val newSet = getState().selectedSigners.toMutableSet()
        if (!checked) {
            newSet.remove(signer)
        } else {
            newSet.add(signer)
        }
        updateSelectedSigners(newSet)
        val state = getState()
        val currentNum = state.totalRequireSigns
        if (currentNum == 0 || state.totalRequireSigns > state.selectedSigners.size) {
            _state.update { it.copy(totalRequireSigns = state.selectedSigners.size) }
        }
    }

    fun handleIncreaseRequiredSigners() {
        val state = getState()
        val currentNum = state.totalRequireSigns
        val newVal =
            if (currentNum + 1 <= state.selectedSigners.size) currentNum + 1 else currentNum
        _state.update { it.copy(totalRequireSigns = newVal) }
    }

    fun handleDecreaseRequiredSigners() {
        val state = getState()
        val currentNum = state.totalRequireSigns
        val newVal = if (currentNum - 1 >= 1) currentNum - 1 else currentNum
        _state.update { it.copy(totalRequireSigns = newVal) }
    }

    fun handleContinueEvent() = viewModelScope.launch {
        val signerMap = getSignerModelMap()
        if (args.addressType.isTaproot() && getState().selectedSigners.size == 1) {
            toggleSelectKeySet(getState().selectedSigners.first())
        }
        val state = getState()
        val hasSigners = state.selectedSigners.isNotEmpty()
        val isValidRequireSigns = state.totalRequireSigns > 0
        if (args.addressType.isTaproot() && state.keySet.isEmpty()) {
            _event.emit(ConfigureWalletEvent.OpenConfigKeySet)
        } else if (isValidRequireSigns && hasSigners) {
            val keySetSigners = if (args.addressType.isTaproot()) {
                state.keySet.mapNotNull { signerMap[it.id] } + state.remoteSigners.filter {
                    state.keySet.contains(
                        it.toModel()
                    )
                }
            } else emptyList()
            val selectedSingleSigners =
                state.selectedSigners.mapNotNull { signerMap[it.id] } + state.remoteSigners.filter {
                    state.selectedSigners.contains(
                        it.toModel()
                    )
                }
            val signers = if (args.addressType.isTaproot()) {
                selectedSingleSigners.sortedBy { signer -> if (keySetSigners.contains(signer)) 0 else 1 }
            } else {
                selectedSingleSigners
            }
            _event.emit(
                ConfigureWalletEvent.AssignSignerCompletedEvent(
                    totalRequireSigns = state.totalRequireSigns,
                    signers = signers,
                )
            )
        }
    }

    fun changeBip32Path(masterSignerId: String, newPath: String) {
        viewModelScope.launch {
            _event.emit(ConfigureWalletEvent.Loading(true))
            val result = getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Params(
                    masterSignerId, newPath
                )
            )
            _event.emit(ConfigureWalletEvent.Loading(false))
            if (result.isSuccess) {
                getSignerModelMap().apply {
                    set(masterSignerId, result.getOrThrow())
                }
                _state.update {
                    it.copy(
                        allSigners = mapSigners(it.masterSigners, it.remoteSigners)
                    )
                }
                updateSelectedSigners(getState().selectedSigners)
                _event.emit(ConfigureWalletEvent.ChangeBip32Success)
            } else {
                _event.emit(ConfigureWalletEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private suspend fun mapSigners(
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    ): List<SignerModel> {
        val signerMap = getSignerModelMap()
        return masterSigners.mapNotNull { masterSigner ->
            val singleSigner = signerMap[masterSigner.id]
            if (masterSigner.type == SignerType.NFC) {
                masterSignerMapper(masterSigner, singleSigner?.derivationPath.orEmpty())
            } else if (singleSigner != null) {
                masterSignerMapper(masterSigner, singleSigner.derivationPath)
            } else null
        } + remoteSigners.map(SingleSigner::toModel)
    }

    fun checkShowRiskSignerDialog() = viewModelScope.launch {
        val hasNonPassphraseSigner =
            getState().selectedSigners.any { selectedSigner -> getState().masterSigners.find { masterSigner -> masterSigner.id == selectedSigner.id }?.device?.needPassPhraseSent == false }

        val isShowRisk =
            signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY && primaryKeySignerInfoHolder.isNeedPassphraseSent()
                .not() && hasNonPassphraseSigner
        _event.emit(ConfigureWalletEvent.ShowRiskSignerDialog(isShowRisk))
    }

    private fun updateSelectedSigners(
        selectedSigners: Set<SignerModel>,
    ) {
        val isSingleSig = selectedSigners.size <= 1
        val signerMap =
            if (isSingleSig) masterSignerSingleMap else masterSignerMutisigMap
        val newSelectedSigner =
            selectedSigners.map {
                val signer = signerMap[it.id]
                if (masterSignerIdSet.contains(it.id) && signer != null) {
                    it.copy(derivationPath = signer.derivationPath, index = signer.index)
                } else {
                    it
                }
            }.toSet()
        _state.update {
            it.copy(
                selectedSigners = newSelectedSigner,
            )
        }
    }

    fun toggleShowPath() {
        _state.update {
            it.copy(isShowPath = it.isShowPath.not())
        }
    }

    private fun getSignerModelMap(): MutableMap<String, SingleSigner> {
        val isSingleSig = getState().selectedSigners.size <= 1
        return if (isSingleSig) masterSignerSingleMap else masterSignerMutisigMap
    }


    fun isShowPath() = getState().isShowPath

    private fun getUnBackedUpWallet() {
        viewModelScope.launch {
            getWallets2UseCase(Unit)
                .onSuccess { wallets ->
                    wallets.filter { it.needBackup }.forEach {
                        unBackedUpSignerXfpSet.add(it.signers.first().masterFingerprint)
                    }
                }
        }
    }

    fun toggleSelectKeySet(signer: SignerModel) {
        val newSet = getState().keySet.toMutableSet()
        if (newSet.contains(signer)) {
            newSet.remove(signer)
        } else {
            newSet.add(signer)
        }
        _state.update {
            it.copy(keySet = newSet)
        }
    }

    fun isUnBackedUpSigner(signer: SignerModel) =
        unBackedUpSignerXfpSet.contains(signer.fingerPrint)

    fun resetKeySet() {
        _state.update {
            it.copy(keySet = emptySet())
        }
    }

    companion object {
        private const val EXTRA_CURRENT_SELECTED_MASTER_SIGNER = "_a"
    }
}

