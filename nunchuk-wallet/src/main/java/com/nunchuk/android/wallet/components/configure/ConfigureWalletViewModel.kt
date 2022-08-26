package com.nunchuk.android.wallet.components.configure

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.toListMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.isContain
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
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
    private val masterSignerMapper: MasterSignerMapper
) : NunchukViewModel<ConfigureWalletState, ConfigureWalletEvent>() {

    private var taproot: Boolean = false

    override val initialState = ConfigureWalletState()

    fun init(taproot: Boolean) {
        this.taproot = taproot
        updateState { initialState.copy(totalRequireSigns = if (taproot) 1 else 0) }
        getSigners()
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute()
            .onStart { event(Loading(true)) }
            .flowOn(Dispatchers.IO)
            .onException {
                updateState {
                    copy(
                        masterSigners = emptyList(),
                        remoteSigners = emptyList()
                    )
                }
            }
            .onEach { updateState { copy(masterSigners = it.first, remoteSigners = it.second) } }
            .flowOn(Dispatchers.Main)
            .onCompletion { event(Loading(false)) }
            .launchIn(viewModelScope)
    }

    fun updateSelectedSigner(signer: SignerModel, checked: Boolean, needPassPhraseSent: Boolean) {
        if (needPassPhraseSent) {
            event(ConfigureWalletEvent.PromptInputPassphrase {
                viewModelScope.launch {
                    sendSignerPassphrase.execute(signer.id, it)
                        .onException { event(ConfigureWalletEvent.InputPassphraseError(it.message.orEmpty())) }
                        .collect { updateStateSelectedSigner(checked, signer, needPassPhraseSent) }
                }
            })
        } else {
            updateStateSelectedSigner(checked, signer, needPassPhraseSent)
        }
    }

    private fun updateStateSelectedSigner(
        checked: Boolean,
        signer: SignerModel,
        needPassPhraseSent: Boolean
    ) {
        var currentNonePassphraseSignerCount = getState().nonePassphraseSignerCount
        if (needPassPhraseSent.not()) {
            if (checked) currentNonePassphraseSignerCount++ else currentNonePassphraseSignerCount --
        }
        updateState {
            copy(
                selectedSigners = if (!checked) {
                    selectedSigners - listOf(signer).toSet()
                } else if (taproot) {
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
                    state.masterSigners.filter {
                        state.selectedSigners.isContain(
                            masterSignerMapper.map(
                                it
                            )
                        )
                    },
                    state.remoteSigners.filter { state.selectedSigners.isContain(it.toModel()) })
            )
        }
    }

    fun mapSigners(): List<SignerModel> {
        val state = getState()
        return masterSignerMapper.toListMapper()(state.masterSigners) + state.remoteSigners.map(
            SingleSigner::toModel
        )
    }

    fun isShowRiskSignerDialog(): Boolean {
        return getState().nonePassphraseSignerCount > 0
    }

}