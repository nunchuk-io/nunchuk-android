package com.nunchuk.android.wallet.assign

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.wallet.assign.AssignSignerEvent.AssignSignerCompletedEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AssignSignerViewModel @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase
) : NunchukViewModel<AssignSignerState, AssignSignerEvent>() {

    override val initialState = AssignSignerState()

    fun init() {
        updateState { initialState }
        getSigners()
    }

    private fun getSigners() {
        viewModelScope.launch {
            val result = getRemoteSignersUseCase.execute()
            updateState { copy(remoteSigners = if (result is Success) result.data else emptyList()) }
        }

        viewModelScope.launch {
            val result = getMasterSignersUseCase.execute()
            updateState { copy(masterSigners = if (result is Success) result.data else emptyList()) }
        }
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
        if (state.totalRequireSigns > 0 && state.remoteSigners.isNotEmpty()) {
            event(
                AssignSignerCompletedEvent(
                    state.totalRequireSigns,
                    state.masterSigners.filter { it.device.masterFingerprint in state.selectedPFXs },
                    state.remoteSigners.filter { it.masterFingerprint in state.selectedPFXs })
            )
        }
    }

}