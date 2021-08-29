package com.nunchuk.android.wallet.shared.components.assign

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.JoinWalletUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class AssignSignerViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val joinWalletUseCase: JoinWalletUseCase
) : NunchukViewModel<AssignSignerState, AssignSignerEvent>() {

    override val initialState = AssignSignerState()

    fun init() {
        updateState { initialState }
        getSigners()
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute()
            .flowOn(Dispatchers.IO)
            .catch { updateState { copy(masterSigners = emptyList(), remoteSigners = emptyList()) } }
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

        if (masterSigners.isEmpty() && remoteSigners.isEmpty()) return

        viewModelScope.launch {
            val unusedSignerSigners = ArrayList<SingleSigner>()
            masterSigners.forEach {
                getUnusedSignerUseCase
                    .execute(it.id, walletType, addressType)
                    .collect { signer -> unusedSignerSigners.add(signer) }
            }

            SessionHolder.currentRoom?.let { room ->
                joinWalletUseCase.execute(room.roomId, remoteSigners + unusedSignerSigners)
                    .flowOn(Dispatchers.IO)
                    .catch { Timber.e(TAG, "init wallet error,", it) }
                    .onEach { event(AssignSignerEvent.AssignSignerCompletedEvent(room.roomId)) }
                    .flowOn(Dispatchers.Main)
                    .launchIn(viewModelScope)
            }
        }
    }

    companion object {
        private const val TAG = "AssignSignerViewModel"
    }

}