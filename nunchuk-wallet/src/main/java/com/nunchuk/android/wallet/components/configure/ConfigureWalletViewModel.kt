package com.nunchuk.android.wallet.components.configure

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

internal class ConfigureWalletViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase
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
            .catch { updateState { copy(masterSigners = emptyList(), remoteSigners = emptyList()) } }
            .onEach { updateState { copy(masterSigners = it.first, remoteSigners = it.second) } }
            .flowOn(Dispatchers.Main)
            .onCompletion { event(Loading(false)) }
            .launchIn(viewModelScope)
    }

    fun updateSelectedXfps(xfp: String, checked: Boolean) {
        updateState {
            copy(
                selectedPFXs = if (checked) selectedPFXs + listOf(xfp) else selectedPFXs - listOf(xfp)
            )
        }
        val state = getState()
        val currentNum = state.totalRequireSigns
        if (currentNum == 0 || state.totalRequireSigns > state.selectedPFXs.size) {
            updateState { copy(totalRequireSigns = state.selectedPFXs.size) }
        }
    }

    fun handleIncreaseRequiredSigners() {
        val state = getState()
        val currentNum = state.totalRequireSigns
        val newVal = if (currentNum + 1 <= state.selectedPFXs.size) currentNum + 1 else currentNum
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
                    state.masterSigners.filter { it.device.masterFingerprint in state.selectedPFXs },
                    state.remoteSigners.filter { it.masterFingerprint in state.selectedPFXs })
            )
        }
    }

}