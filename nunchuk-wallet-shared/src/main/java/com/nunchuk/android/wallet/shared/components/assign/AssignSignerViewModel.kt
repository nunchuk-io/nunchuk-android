package com.nunchuk.android.wallet.shared.components.assign

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.usecase.InitWalletUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

internal class AssignSignerViewModel @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase,
    private val initWalletUseCase: InitWalletUseCase
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

    fun handleContinueEvent(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int
    ) {
        SessionHolder.currentRoom?.roomId?.let {
            initWallet(
                it,
                walletName,
                walletType,
                addressType,
                totalSigns,
                requireSigns
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
            .catch { Log.e(TAG, "init wallet error,", it) }
            .onEach { Log.e(TAG, "init wallet completed,") }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    companion object {
        private const val TAG = "AssignSignerViewModel"
    }

}