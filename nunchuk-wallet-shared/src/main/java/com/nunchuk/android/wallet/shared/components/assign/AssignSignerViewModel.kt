package com.nunchuk.android.wallet.shared.components.assign

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.domain.SendErrorEventUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.toListMapper
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.JoinWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.shared.components.assign.AssignSignerEvent.AssignSignerErrorEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AssignSignerViewModel @AssistedInject constructor(
    @Assisted private val args: AssignSignerArgs,
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val joinWalletUseCase: JoinWalletUseCase,
    private val sendErrorEventUseCase: SendErrorEventUseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val sessionHolder: SessionHolder,
    private val masterSignerMapper: MasterSignerMapper
) : NunchukViewModel<AssignSignerState, AssignSignerEvent>() {

    override val initialState = AssignSignerState()

    fun init() {
        updateState { initialState }
        getSigners()
    }

    fun filterSigners(signer: SingleSigner) {
        hasSignerUseCase.execute(signer)
            .flowOn(Dispatchers.IO)
            .onException {  }
            .onEach {
                if (it) {
                    updateState {
                        val newList = filterRecSigners.toMutableList()
                        newList.add(signer)
                        copy(
                            filterRecSigners = newList
                        )
                    }
                }
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
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
        val remoteSigners = if (state.filterRecSigners.isNotEmpty()) state.filterRecSigners.filter { it.masterFingerprint in state.selectedPFXs } else state.remoteSigners.filter { it.masterFingerprint in state.selectedPFXs }

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

            if (sessionHolder.hasActiveRoom()) {
                sessionHolder.getActiveRoomId().let { roomId ->
                    joinWalletUseCase.execute(
                        roomId,
                        if (state.filterRecSigners.isNotEmpty()) remoteSigners else remoteSigners + unusedSignerSigners
                    )
                        .flowOn(Dispatchers.IO)
                        .onException {
                            event(AssignSignerErrorEvent(it.readableMessage()))
                            sendErrorEvent(roomId, it, sendErrorEventUseCase::execute)
                        }
                        .onEach { event(AssignSignerEvent.AssignSignerCompletedEvent(roomId)) }
                        .flowOn(Dispatchers.Main)
                        .launchIn(viewModelScope)
                }
            }
        }
    }

    fun mapSigners(): List<SignerModel> {
        val state = getState()
        val signers = if (args.signers.isNotEmpty()) {
            masterSignerMapper.toListMapper()(state.masterSigners) + state.filterRecSigners.map(SingleSigner::toModel)
        } else {
            masterSignerMapper.toListMapper()(state.masterSigners) + state.remoteSigners.map(SingleSigner::toModel)
        }
        return signers
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: AssignSignerArgs): AssignSignerViewModel
    }
}