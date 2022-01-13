package com.nunchuk.android.wallet.components.configure

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.isContain
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.Loading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConfigureWalletViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase
) : NunchukViewModel<ConfigureWalletState, ConfigureWalletEvent>() {

    override val initialState = ConfigureWalletState()

    fun init() {
        updateState { initialState }
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
                        .collect { updateStateSelectedSigner(checked, signer) }
                }
            })
            return
        }

        updateStateSelectedSigner(checked, signer)
    }

    private fun updateStateSelectedSigner(
        checked: Boolean,
        signer: SignerModel
    ) {
        updateState {
            copy(
                selectedSigners = if (checked) selectedSigners + listOf(signer) else selectedSigners - listOf(
                    signer
                )
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
                    state.masterSigners.filter { state.selectedSigners.isContain(it.toModel()) },
                    state.remoteSigners.filter { state.selectedSigners.isContain(it.toModel()) })
            )
        }
    }

}