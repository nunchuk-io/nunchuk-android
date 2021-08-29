package com.nunchuk.android.wallet.shared.components.assign

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.InitWalletUseCase
import com.nunchuk.android.usecase.JoinWalletUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class AssignSignerViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val joinWalletUseCase: JoinWalletUseCase,
    private val initWalletUseCase: InitWalletUseCase
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

    fun handleContinueEvent(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int
    ) {
        SessionHolder.currentRoom?.roomId?.let {
            initWallet(
                roomId = it,
                walletName = walletName,
                walletType = walletType,
                addressType = addressType,
                totalSigns = totalSigns,
                requireSigns = requireSigns
            )
        }
    }

    private fun initWallet(
        roomId: String,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int
    ) {
        initWalletUseCase.execute(
            roomId = roomId,
            name = walletName,
            totalSigns = totalSigns,
            requireSigns = requireSigns,
            addressType = addressType,
            isEscrow = walletType == WalletType.ESCROW
        )
            .flowOn(Dispatchers.IO)
            .catch { Timber.e(TAG, "init wallet error,", it) }
            .onEach {
                Timber.d(TAG, "init wallet completed $it")
                doAssignSigners(walletType, addressType)
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    private fun doAssignSigners(walletType: WalletType, addressType: AddressType) {
        val state = getState()
        val masterSigners = state.masterSigners
        val remoteSigners = state.remoteSigners
        viewModelScope.launch {
            val unusedSignerSigners = ArrayList<SingleSigner>()
            masterSigners.forEach {
                getUnusedSignerUseCase
                    .execute(it.id, walletType, addressType)
                    .collect { signer -> unusedSignerSigners.add(signer) }
            }

            SessionHolder.currentRoom?.let {
                joinWalletUseCase.execute(it.roomId, remoteSigners + unusedSignerSigners)
                    .flowOn(Dispatchers.IO)
                    .catch { t -> Timber.e(TAG, "init wallet error,", t) }
                    .onEach { doAssignSigners(walletType, addressType) }
                    .flowOn(Dispatchers.Main)
                    .launchIn(viewModelScope)
            }
        }
    }

    companion object {
        private const val TAG = "AssignSignerViewModel"
    }

}