package com.nunchuk.android.wallet.components.configure

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.isContain
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
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
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase
) : NunchukViewModel<ConfigureWalletState, ConfigureWalletEvent>() {

    private lateinit var args: ConfigureWalletArgs

    override val initialState = ConfigureWalletState()

    fun init(args: ConfigureWalletArgs) {
        this.args = args
        updateState { initialState.copy(totalRequireSigns = if (args.addressType.isTaproot()) 1 else 0) }
        getSigners()
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute()
            .onStart { event(Loading(true)) }
            .onException {
                updateState {
                    copy(
                        masterSigners = emptyList(),
                        remoteSigners = emptyList()
                    )
                }
            }.flatMapLatest { signerPair ->
                getUnusedSignerFromMasterSignerUseCase.execute(
                    signerPair.first,
                    args.walletType,
                    args.addressType
                ).map { signers ->
                    Triple(
                        signerPair.first,
                        signerPair.second,
                        signers.associateBy { it.masterSignerId })
                }
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                updateState {
                    copy(
                        masterSigners = it.first,
                        masterSignerMap = it.third,
                        remoteSigners = it.second,
                    )
                }
            }
            .flowOn(Dispatchers.Main)
            .onCompletion { event(Loading(false)) }
            .launchIn(viewModelScope)
    }

    fun updateSelectedSigner(signer: SignerModel, checked: Boolean, needPassPhraseSent: Boolean) {
        if (needPassPhraseSent) {
            event(ConfigureWalletEvent.PromptInputPassphrase {
                viewModelScope.launch {
                    sendSignerPassphrase.execute(signer.id, it)
                        .onException { event(ConfigureWalletEvent.ShowError(it.message.orEmpty())) }
                        .collect { updateStateSelectedSigner(checked, signer, true) }
                }
            })
        } else {
            updateStateSelectedSigner(checked, signer, false)
        }
    }

    private fun updateStateSelectedSigner(
        checked: Boolean,
        signer: SignerModel,
        needPassPhraseSent: Boolean
    ) {
        var currentNonePassphraseSignerCount = getState().nonePassphraseSignerCount
        if (needPassPhraseSent.not()) {
            if (checked) currentNonePassphraseSignerCount++ else currentNonePassphraseSignerCount--
        }
        updateState {
            copy(
                selectedSigners = if (!checked) {
                    selectedSigners - listOf(signer).toSet()
                } else if (args.addressType.isTaproot()) {
                    listOf(signer)
                } else {
                    selectedSigners + listOf(signer)
                },
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
                AssignSignerCompletedEvent(
                    state.totalRequireSigns,
                    state.selectedSigners
                        .asSequence()
                        .map {
                            state.masterSignerMap[it.id]
                        }.filterNotNull()
                        .toList(),
                    state.remoteSigners.filter { state.selectedSigners.isContain(it.toModel()) })
            )
        }
    }

    fun changeBip32Path(masterSignerId: String, newPath: String) {
        viewModelScope.launch {
            setEvent(Loading(true))
            val result = getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Params(
                    masterSignerId,
                    newPath
                )
            )
            setEvent(Loading(false))
            if (result.isSuccess) {
                val newMap = getState().masterSignerMap.toMutableMap().apply {
                    set(masterSignerId, result.getOrThrow())
                }
                updateState {
                    copy(masterSignerMap = newMap)
                }
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
                signer,
                state.masterSignerMap[signer.id]?.derivationPath.orEmpty()
            )
        } + state.remoteSigners.map(SingleSigner::toModel)
    }

    fun isShowRiskSignerDialog(): Boolean {
        return getState().nonePassphraseSignerCount > 0
    }

}