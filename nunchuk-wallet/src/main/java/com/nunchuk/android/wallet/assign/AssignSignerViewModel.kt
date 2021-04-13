package com.nunchuk.android.wallet.assign

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.wallet.assign.AssignSignerEvent.AssignSignerCompletedEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AssignSignerViewModel @Inject constructor(
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
            updateState { copy(signers = if (result is Result.Success) result.data else emptyList()) }
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

    fun updateTotalRequireSigns(number: String) {
        val state = getState()
        val numberVal = if (number.isBlank()) 0 else number.toInt()
        if (numberVal != state.totalRequireSigns) {
            val newVal: Int = when {
                numberVal <= 0 -> 0
                numberVal >= state.selectedPFXs.size -> state.selectedPFXs.size
                else -> numberVal
            }
        }
    }

    fun handleContinueEvent() {
        val state = getState()
        if (state.totalRequireSigns > 0 && state.signers.isNotEmpty()) {
            event(AssignSignerCompletedEvent(state.totalRequireSigns, state.signers.filter { it.masterFingerprint in state.selectedPFXs }))
        }
    }

}