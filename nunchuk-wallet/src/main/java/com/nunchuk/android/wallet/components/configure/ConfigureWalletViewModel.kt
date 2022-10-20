package com.nunchuk.android.wallet.components.configure

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.GetDefaultSignerFromMasterSignerUseCase
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
    private val getDefaultSignerFromMasterSignerUseCase: GetDefaultSignerFromMasterSignerUseCase
) : NunchukViewModel<ConfigureWalletState, ConfigureWalletEvent>() {

    private lateinit var args: ConfigureWalletArgs

    private var walletType = WalletType.SINGLE_SIG

    override val initialState = ConfigureWalletState()

    private val masterSignerIdSet = mutableSetOf<String>()

    fun init(args: ConfigureWalletArgs) {
        this.args = args
        updateState { initialState.copy(totalRequireSigns = if (args.addressType.isTaproot()) 1 else 0) }
        getSigners()
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute().onStart { event(Loading(true)) }.onException {
            updateState {
                copy(
                    masterSigners = emptyList(), remoteSigners = emptyList()
                )
            }
        }.flatMapLatest { signerPair ->
            masterSignerIdSet.addAll(signerPair.first.map { it.id })
            getUnusedSignerFromMasterSignerUseCase.execute(
                signerPair.first, WalletType.SINGLE_SIG, args.addressType
            ).map { signers ->
                Triple(
                    signerPair.first,
                    signerPair.second,
                    signers.associateBy { it.masterSignerId })
            }
        }.flowOn(Dispatchers.IO).onEach {
            updateState {
                copy(
                    masterSigners = it.first,
                    masterSignerMap = it.third,
                    remoteSigners = it.second,
                )
            }
        }.flowOn(Dispatchers.Main).onCompletion { event(Loading(false)) }.launchIn(viewModelScope)
    }

    fun reloadSignerPath() {
        val newWalletType =
            if (getState().selectedSigners.size > 1) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
        if (newWalletType != walletType) {
            walletType = newWalletType
            viewModelScope.launch {
                getUnusedSignerFromMasterSignerUseCase.execute(
                    getState().masterSigners, newWalletType, args.addressType
                ).collect { signers ->
                    // the path change so we need to map selected signers to new path
                    val newSignerMap = signers.associateBy { it.masterSignerId }
                    val signer: SignerModel? =
                        savedStateHandle[EXTRA_CURRENT_SELECTED_MASTER_SIGNER]
                    // if user select new signer but we can not get derivationPath it should tap again
                    if (signer != null && signer.type == SignerType.NFC && newSignerMap.contains(
                            signer.id
                        ).not()
                    ) {
                        setEvent(ConfigureWalletEvent.RequestCacheTapSignerXpub(signer))
                    }
                    handleNewPathMap(newSignerMap)
                }
            }
        }
    }

    fun cacheTapSignerXpub(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            val signer: SignerModel =
                savedStateHandle[EXTRA_CURRENT_SELECTED_MASTER_SIGNER] ?: return@launch
            val result = cacheDefaultTapsignerMasterSignerXPubUseCase(
                CacheDefaultTapsignerMasterSignerXPubUseCase.Data(isoDep, cvc, signer.id)
            )
            if (result.isSuccess) {
                val newWalletType =
                    if (getState().selectedSigners.size > 1) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
                val signerResult = getDefaultSignerFromMasterSignerUseCase(
                    GetDefaultSignerFromMasterSignerUseCase.Params(
                        signer.id, newWalletType, args.addressType
                    )
                )
                if (signerResult.isSuccess) {
                    val newSigner = signerResult.getOrThrow()
                    updateStateSelectedSigner(
                        true,
                        newSigner.toModel(),
                        false
                    )
                    updateState {
                        copy(masterSignerMap = masterSignerMap.toMutableMap().apply {
                            set(signer.id, newSigner)
                        })
                    }
                } else {
                    updateStateSelectedSigner(false, signer, false)
                    setEvent(ConfigureWalletEvent.CacheTapSignerXpubError(result.exceptionOrNull()))
                }
            } else {
                updateStateSelectedSigner(false, signer, false)
                setEvent(ConfigureWalletEvent.CacheTapSignerXpubError(result.exceptionOrNull()))
            }
        }
    }

    fun updateSelectedSigner(signer: SignerModel, checked: Boolean) {
        val masterSigner =
            getState().masterSigners.find { it.device.masterFingerprint == signer.fingerPrint }
        savedStateHandle[EXTRA_CURRENT_SELECTED_MASTER_SIGNER] = signer
        val device = masterSigner?.device
        val isShouldCacheXpub = signer.type == SignerType.NFC && signer.derivationPath.isEmpty()
        if (checked && isShouldCacheXpub) {
            setEvent(ConfigureWalletEvent.RequestCacheTapSignerXpub(signer))
        } else if (checked && device?.needPassPhraseSent == true) {
            setEvent(ConfigureWalletEvent.PromptInputPassphrase(signer))
        } else {
            updateStateSelectedSigner(checked, signer, false)
        }
    }

    fun verifyPassphrase(signer: SignerModel, passphrase: String) {
        viewModelScope.launch {
            sendSignerPassphrase.execute(signer.id, passphrase).onException {
                event(ConfigureWalletEvent.ShowError(it.message.orEmpty()))
                updateStateSelectedSigner(false, signer, true)
            }.collect { updateStateSelectedSigner(true, signer, true) }
        }
    }

    fun cancelVerifyPassphrase(signer: SignerModel) {
        updateStateSelectedSigner(false, signer, true)
    }

    private fun updateStateSelectedSigner(
        checked: Boolean, signer: SignerModel, needPassPhraseSent: Boolean
    ) {
        var currentNonePassphraseSignerCount = getState().nonePassphraseSignerCount
        if (needPassPhraseSent.not()) {
            if (checked) currentNonePassphraseSignerCount++ else currentNonePassphraseSignerCount--
        }
        val newSet = getState().selectedSigners.toMutableSet()
        if (!checked) {
            newSet.remove(signer)
        } else if (args.addressType.isTaproot()) {
            newSet.clear()
            newSet.add(signer)
        } else {
            newSet.add(signer)
        }

        updateState {
            copy(
                selectedSigners = newSet,
                nonePassphraseSignerCount = currentNonePassphraseSignerCount
            )
        }
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
        if (isValidRequireSigns && hasSigners) {
            event(
                AssignSignerCompletedEvent(state.totalRequireSigns,
                    state.selectedSigners.asSequence().map {
                        state.masterSignerMap[it.id]
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
                val newSignerMap = getState().masterSignerMap.toMutableMap().apply {
                    set(masterSignerId, result.getOrThrow())
                }
                handleNewPathMap(newSignerMap)
                setEvent(ConfigureWalletEvent.ChangeBip32Success)
            } else {
                setEvent(ConfigureWalletEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun mapSigners(): List<SignerModel> {
        val state = getState()
        return state.masterSigners.map { signer ->
            masterSignerMapper(
                signer, state.masterSignerMap[signer.id]?.derivationPath.orEmpty()
            )
        } + state.remoteSigners.map(SingleSigner::toModel)
    }

    fun checkShowRiskSignerDialog() = viewModelScope.launch {
        if (signInModeHolder.getCurrentMode() != SignInMode.PRIMARY_KEY || getState().nonePassphraseSignerCount == 0 || primaryKeySignerInfoHolder.isNeedPassphraseSent()) {
            setEvent(ConfigureWalletEvent.ShowRiskSignerDialog(false))
            return@launch
        }
        setEvent(ConfigureWalletEvent.ShowRiskSignerDialog(true))
    }

    private fun handleNewPathMap(newSignerMap: Map<String, SingleSigner>) {
        val selectedSigners =
            getState().selectedSigners.mapNotNull {
                if (masterSignerIdSet.contains(it.id)) {
                    newSignerMap[it.id]?.takeIf { signer -> signer.derivationPath.isNotEmpty() }
                        ?.toModel(true)
                } else {
                    it
                }
            }.toSet()
        updateState {
            copy(
                masterSignerMap = newSignerMap,
                selectedSigners = selectedSigners
            )
        }
    }

    fun toggleShowPath() {
        updateState {
            copy(
                isShowPath = isShowPath.not()
            )
        }
    }

    fun isShowPath() = getState().isShowPath

    companion object {
        private const val EXTRA_CURRENT_SELECTED_MASTER_SIGNER = "_a"
    }
}