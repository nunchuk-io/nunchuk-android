package com.nunchuk.android.wallet.shared.components.configure

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.wallet.shared.components.configure.ConfigureSharedWalletEvent.ConfigureCompletedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import javax.inject.Inject

@HiltViewModel
internal class ConfigureSharedWalletViewModel @Inject constructor(
    private val sessionHolder: SessionHolder,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : NunchukViewModel<ConfigureSharedWalletState, ConfigureSharedWalletEvent>() {

    override val initialState = ConfigureSharedWalletState()

    fun init() {
        viewModelScope.launch {
            updateState { initialState }
            val result = withContext(dispatcher) {
                sessionHolder.getCurrentRoom()?.membershipService()?.getRoomMembers(roomMemberQueryParams())
            }
            result?.let {
                updateState { copy(minTotalSigner = it.size, totalSigns = it.size) }
            }
        }
    }

    fun handleIncreaseTotalSigners() {
        val state = getState()
        updateState { copy(totalSigns = state.totalSigns + 1) }
        getState().validate()
    }

    fun handleDecreaseTotalSigners() {
        val state = getState()
        val totalSigns = state.totalSigns
        val minus = totalSigns - 1
        val newVal = if (minus >= state.minTotalSigner && minus >= state.requireSigns) minus else totalSigns
        updateState { copy(totalSigns = newVal) }
        getState().validate()
    }

    fun handleIncreaseRequiredSigners() {
        val state = getState()
        val currentNum = state.requireSigns
        val newVal = if (currentNum + 1 <= state.totalSigns) currentNum + 1 else currentNum
        updateState { copy(requireSigns = newVal) }
        getState().validate()
    }

    fun handleDecreaseRequiredSigners() {
        val state = getState()
        val currentNum = state.requireSigns
        val newVal = if (currentNum - 1 >= 0) currentNum - 1 else currentNum
        updateState { copy(requireSigns = newVal) }
        getState().validate()
    }

    fun handleContinue() {
        val state = getState()
        event(ConfigureCompletedEvent(state.totalSigns, state.requireSigns))
    }

    private fun ConfigureSharedWalletState.validate() {
        val isConfigured = (requireSigns > 0) && (requireSigns <= totalSigns)
        val canDecreaseTotal = totalSigns > getState().minTotalSigner
        updateState { copy(isConfigured = isConfigured, canDecreaseTotal = canDecreaseTotal) }
    }
}