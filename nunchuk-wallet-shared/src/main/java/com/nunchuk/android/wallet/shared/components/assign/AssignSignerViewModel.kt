package com.nunchuk.android.wallet.shared.components.assign

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.wallet.shared.components.assign.ConfigureWalletEvent.AssignSignerCompletedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

internal class AssignSignerViewModel @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase
) : NunchukViewModel<ConfigureWalletState, ConfigureWalletEvent>() {

    override val initialState = ConfigureWalletState()

    fun init() {
        updateState { initialState }
        getSigners()
    }

    private fun getSigners() {
        getRemoteSignersUseCase.execute()
            .flowOn(Dispatchers.IO)
            .catch { updateState { copy(remoteSigners = emptyList()) } }
            .onEach { updateState { copy(remoteSigners = it) } }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)

        getMasterSignersUseCase.execute()
            .flowOn(Dispatchers.IO)
            .catch { updateState { copy(masterSigners = emptyList()) } }
            .onEach { updateState { copy(masterSigners = it) } }
            .flowOn(Dispatchers.Main)
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