package com.nunchuk.android.wallet.personal.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.ImportWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class RecoverWalletViewModel @Inject constructor(
    private val importWalletUseCase: ImportWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val getWalletUseCase: GetWalletUseCase
) : NunchukViewModel<RecoverWalletState, RecoverWalletEvent>() {

    override val initialState = RecoverWalletState()

    val walletName: String?
        get() = state.value?.walletName

    fun init() {
        updateState { initialState }
    }

    fun importWallet(filePath: String, name: String, description: String) {
        viewModelScope.launch {
            importWalletUseCase.execute(filePath, name, description)
                .flowOn(Dispatchers.IO)
                .onException { event(RecoverWalletEvent.ImportWalletErrorEvent(it.readableMessage())) }
                .flowOn(Dispatchers.Main)
                .collect { event(RecoverWalletEvent.ImportWalletSuccessEvent(it.id, it.name)) }
        }
    }

    fun updateWalletName(walletName: String) {
        updateState { copy(walletName = walletName) }
    }

    fun updateWallet(walletId: String, walletName: String) {
        getWalletUseCase.execute(walletId)
            .flowOn(Dispatchers.IO)
            .onException { event(RecoverWalletEvent.UpdateWalletErrorEvent(it.message.orEmpty())) }
            .flatMapConcat {
                updateWalletUseCase.execute(it.wallet.copy(name = walletName))
                    .flowOn(Dispatchers.IO)
                    .onException { err ->
                        event(RecoverWalletEvent.UpdateWalletErrorEvent(err.message.orEmpty()))
                    }
            }.onEach {
                event(RecoverWalletEvent.UpdateWalletSuccessEvent(walletId, walletName))
            }.flowOn(Dispatchers.Main).launchIn(viewModelScope)
    }

    fun handleContinueEvent() {
        val currentState = getState()
        if (currentState.walletName.isNotEmpty()) {
            event(RecoverWalletEvent.WalletSetupDoneEvent(walletName = currentState.walletName))
        } else {
            event(RecoverWalletEvent.WalletNameRequiredEvent)
        }
    }

}