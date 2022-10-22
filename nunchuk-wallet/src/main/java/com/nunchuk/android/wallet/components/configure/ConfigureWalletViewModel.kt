package com.nunchuk.android.wallet.components.configure

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.domain.CacheDefaultTapsignerMasterSignerXPubUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
internal class ConfigureWalletViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getUnusedSignerFromMasterSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
    private val savedStateHandle: SavedStateHandle,
    private val cacheDefaultTapsignerMasterSignerXPubUseCase: CacheDefaultTapsignerMasterSignerXPubUseCase,
) : NunchukViewModel<ConfigureWalletState, ConfigureWalletEvent>() {

    private lateinit var args: ConfigureWalletArgs

    override val initialState = ConfigureWalletState()

    private val masterSignerIdSet = mutableSetOf<String>()

    fun init(args: ConfigureWalletArgs) {
        this.args = args
        updateState { initialState.copy(totalRequireSigns = if (args.addressType.isTaproot()) 1 else 0) }
        getSigners()
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute()
            .onException {}
            .map {
                // TAPROOT only support software key
                if (args.addressType == AddressType.TAPROOT) {
                    Pair(
                        it.first.filter { signer -> signer.type == SignerType.SOFTWARE },
                        it.second.filter { signer -> signer.type == SignerType.SOFTWARE },
                    )
                } else {
                    it
                }
            }.map { signerPair ->
                masterSignerIdSet.addAll(signerPair.first.map { it.id })
                val singleSigSigners = getUnusedSignerFromMasterSignerUseCase.execute(
                    signerPair.first, WalletType.SINGLE_SIG, args.addressType
                ).first()
                val multiSigSigners = getUnusedSignerFromMasterSignerUseCase.execute(
                    signerPair.first, WalletType.MULTI_SIG, args.addressType
                ).first()
                val multiSigSignersMap = multiSigSigners.associateBy { it.masterSignerId }
                val singleSigSignersMap = singleSigSigners.associateBy { it.masterSignerId }
                ConfigureWalletState(
                    masterSigners = signerPair.first,
                    remoteSigners = signerPair.second,
                    masterSignerSingleMap = singleSigSignersMap,
                    masterSignerMutisigMap = multiSigSignersMap
                )
            }
            .flowOn(Dispatchers.IO)
            .onEach { state ->
                updateState { state.copy(selectedSigners = selectedSigners) }
            }.flowOn(Dispatchers.Main)
            .onStart { setEvent(Loading(true)) }
            .onCompletion { setEvent(Loading(false)) }
            .launchIn(viewModelScope)
    }

    fun cacheTapSignerXpub(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            val signer: SignerModel =
                savedStateHandle[EXTRA_CURRENT_SELECTED_MASTER_SIGNER] ?: return@launch
            val result = cacheDefaultTapsignerMasterSignerXPubUseCase(
                CacheDefaultTapsignerMasterSignerXPubUseCase.Data(isoDep, cvc, signer.id)
            )
            if (result.isSuccess) {
                updateStateSelectedSigner(true, signer)
                getSigners()
            } else {
                updateStateSelectedSigner(false, signer)
                setEvent(ConfigureWalletEvent.CacheTapSignerXpubError(result.exceptionOrNull()))
            }
        }
    }

    fun updateSelectedSigner(signer: SignerModel, checked: Boolean) {
        val masterSigner =
            getState().masterSigners.find { it.device.masterFingerprint == signer.fingerPrint }
        savedStateHandle[EXTRA_CURRENT_SELECTED_MASTER_SIGNER] = signer
        val device = masterSigner?.device
        val isShouldCacheXpub = signer.type == SignerType.NFC
                && !(getState().masterSignerSingleMap.contains(signer.id) && getState().masterSignerMutisigMap.contains(
            signer.id
        ))
        if (checked && isShouldCacheXpub) {
            setEvent(ConfigureWalletEvent.RequestCacheTapSignerXpub(signer))
        } else if (checked && device?.needPassPhraseSent == true) {
            setEvent(ConfigureWalletEvent.PromptInputPassphrase(signer))
        } else {
            updateStateSelectedSigner(checked, signer)
        }
    }

    fun verifyPassphrase(signer: SignerModel, passphrase: String) {
        viewModelScope.launch {
            sendSignerPassphrase.execute(signer.id, passphrase).onException {
                event(ConfigureWalletEvent.ShowError(it.message.orEmpty()))
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
        } else if (args.addressType.isTaproot()) {
            newSet.clear()
            newSet.add(signer)
        } else {
            newSet.add(signer)
        }

        val isSingleSig = newSet.size <= 1
        val signerMap = if (isSingleSig) getState().masterSignerSingleMap else getState().masterSignerMutisigMap
        updateSelectedSigners(newSet, signerMap)
        val state = getState()
        val currentNum = state.totalRequireSigns
        if (currentNum == 0 || state.totalRequireSigns > state.selectedSigners.size) {
            updateState { copy(totalRequireSigns = state.selectedSigners.size) }
        }
    }

    fun handleIncreaseRequiredSigners() {
        val state = getState()
        val currentNum = state.totalRequireSigns
        val newVal =
            if (currentNum + 1 <= state.selectedSigners.size) currentNum + 1 else currentNum
        updateState { copy(totalRequireSigns = newVal) }
    }

    fun handleDecreaseRequiredSigners() {
        val state = getState()
        val currentNum = state.totalRequireSigns
        val newVal = if (currentNum - 1 >= 0) currentNum - 1 else currentNum
        updateState { copy(totalRequireSigns = newVal) }
    }

    fun handleContinueEvent() {
        val state = getState()
        val hasSigners = state.remoteSigners.isNotEmpty() || state.masterSigners.isNotEmpty()
        val isValidRequireSigns = state.totalRequireSigns > 0
        val signerMap = getSignerModelMap()
        if (isValidRequireSigns && hasSigners) {
            event(
                AssignSignerCompletedEvent(state.totalRequireSigns,
                    state.selectedSigners.asSequence().map {
                        signerMap[it.id]
                    }.filterNotNull().toList(),
                    state.remoteSigners.filter { state.selectedSigners.contains(it.toModel()) })
            )
        }
    }

    fun changeBip32Path(masterSignerId: String, newPath: String) {
        viewModelScope.launch {
            setEvent(Loading(true))
            val result = getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Params(
                    masterSignerId, newPath
                )
            )
            setEvent(Loading(false))
            if (result.isSuccess) {
                val newSignerMap = getSignerModelMap().toMutableMap().apply {
                    set(masterSignerId, result.getOrThrow())
                }
                updateSelectedSigners(getState().selectedSigners, newSignerMap)
                setEvent(ConfigureWalletEvent.ChangeBip32Success)
            } else {
                setEvent(ConfigureWalletEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun mapSigners(): List<SignerModel> {
        val state = getState()
        val signerMap = getSignerModelMap()
        return state.masterSigners.map { signer ->
            masterSignerMapper(
                signer, signerMap[signer.id]?.derivationPath.orEmpty()
            )
        } + state.remoteSigners.map(SingleSigner::toModel)
    }

    fun checkShowRiskSignerDialog() = viewModelScope.launch {
        val hasNonPassphraseSigner =
            getState().selectedSigners.any { selectedSigner -> getState().masterSigners.find { masterSigner -> masterSigner.id == selectedSigner.id }?.device?.needPassPhraseSent == false }

        val isShowRisk = signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY && primaryKeySignerInfoHolder.isNeedPassphraseSent()
            .not() && hasNonPassphraseSigner
        setEvent(ConfigureWalletEvent.ShowRiskSignerDialog(isShowRisk))
    }

    private fun updateSelectedSigners(
        selectedSigners: Set<SignerModel>,
        signerMap: Map<String, SingleSigner>
    ) {
        val newSelectedSigner =
            selectedSigners.mapNotNull {
                if (masterSignerIdSet.contains(it.id)) {
                    signerMap[it.id]?.toModel()
                } else {
                    it
                }
            }.toSet()
        val isSingleSig = selectedSigners.size <= 1
        if (isSingleSig) {
            updateState {
                copy(
                    masterSignerSingleMap = signerMap,
                    selectedSigners = newSelectedSigner
                )
            }
        } else {
            updateState {
                copy(
                    masterSignerMutisigMap = signerMap,
                    selectedSigners = newSelectedSigner
                )
            }
        }
    }

    fun toggleShowPath() {
        updateState {
            copy(
                isShowPath = isShowPath.not()
            )
        }
    }

    private fun getSignerModelMap() : Map<String, SingleSigner> {
        val isSingleSig = getState().selectedSigners.size <= 1
        return if (isSingleSig) getState().masterSignerSingleMap else getState().masterSignerMutisigMap
    }


    fun isShowPath() = getState().isShowPath

    companion object {
        private const val EXTRA_CURRENT_SELECTED_MASTER_SIGNER = "_a"
    }
}

