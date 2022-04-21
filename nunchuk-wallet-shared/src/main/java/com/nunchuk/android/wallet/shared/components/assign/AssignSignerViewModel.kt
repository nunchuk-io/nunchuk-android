package com.nunchuk.android.wallet.shared.components.assign

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.SendErrorEventUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.JoinWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.shared.components.assign.AssignSignerEvent.AssignSignerErrorEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AssignSignerViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val joinWalletUseCase: JoinWalletUseCase,
    private val sendErrorEventUseCase: SendErrorEventUseCase
) : NunchukViewModel<AssignSignerState, AssignSignerEvent>() {

    override val initialState = AssignSignerState()

    fun init() {
        updateState { initialState }
        getSigners()
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute()
            .flowOn(Dispatchers.IO)
            .onException { updateState { copy(masterSigners = emptyList(), remoteSigners = emptyList()) } }
            .onEach { updateState { copy(masterSigners = it.first, remoteSigners = it.second) } }
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

    fun handleContinueEvent(walletType: WalletType, addressType: AddressType) {
        val state = getState()
        val masterSigners = state.masterSigners.filter { it.device.masterFingerprint in state.selectedPFXs }
        val remoteSigners = state.remoteSigners.filter { it.masterFingerprint in state.selectedPFXs }

        if (masterSigners.isEmpty() && remoteSigners.isEmpty()) {
            event(AssignSignerErrorEvent("No keys found"))
            return
        }

        viewModelScope.launch {
            val unusedSignerSigners = ArrayList<SingleSigner>()
            getUnusedSignerUseCase
                .execute(masterSigners, walletType, addressType)
                .onException {}
                .collect { unusedSignerSigners.addAll(it) }

            SessionHolder.currentRoom?.let { room ->
                joinWalletUseCase.execute(room.roomId, remoteSigners + unusedSignerSigners)
                    .flowOn(Dispatchers.IO)
                    .onException {
                        event(AssignSignerErrorEvent(it.readableMessage()))
                        sendErrorEvent(room.roomId, it, sendErrorEventUseCase::execute)
                    }
                    .onEach { event(AssignSignerEvent.AssignSignerCompletedEvent(room.roomId)) }
                    .flowOn(Dispatchers.Main)
                    .launchIn(viewModelScope)
            }
        }
    }

}