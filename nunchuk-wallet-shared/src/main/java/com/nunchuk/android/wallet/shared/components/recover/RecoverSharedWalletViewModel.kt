package com.nunchuk.android.wallet.shared.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.ParseWalletDescriptorUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RecoverSharedWalletViewModel @Inject constructor(
    private val parseWalletDescriptorUseCase: ParseWalletDescriptorUseCase
) : NunchukViewModel<RecoverSharedWalletState, RecoverSharedWalletEvent>() {
    val walletName: String?
        get() = state.value?.walletName

    override val initialState = RecoverSharedWalletState()

    fun updateWalletName(walletName: String) {
        updateState { copy(walletName = walletName) }
    }

    fun handleContinueEvent() {
        val currentState = getState()
        if (currentState.walletName.isNotEmpty()) {
            event(RecoverSharedWalletEvent.WalletSetupDoneEvent(walletName = currentState.walletName))
        } else {
            event(RecoverSharedWalletEvent.WalletNameRequiredEvent)
        }
    }

    fun parseWalletDescriptor(content: String) {
        viewModelScope.launch {
            parseWalletDescriptorUseCase.execute(content)
                .flowOn(Dispatchers.IO)
                .onException {  }
                .flowOn(Dispatchers.Main)
                .collect {
                    event(RecoverSharedWalletEvent.RecoverSharedWalletSuccess(it))
                }
        }
    }

}