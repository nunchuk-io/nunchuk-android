package com.nunchuk.android.wallet.config

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.wallet.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.config.WalletConfigEvent.UpdateNameSuccessEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletConfigViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase
) : NunchukViewModel<Wallet, WalletConfigEvent>() {

    override val initialState = Wallet()

    lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
        getWalletDetails()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            when (val result = getWalletUseCase.execute(walletId)) {
                is Result.Success -> updateState { result.data }
                is Result.Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }

    fun handleEditCompleteEvent(walletName: String) {
        viewModelScope.launch {
            when (val result = updateWalletUseCase.execute(getState().copy(name = walletName))) {
                is Result.Success -> {
                    updateState { copy(name = walletName) }
                    event(UpdateNameSuccessEvent)
                }
                is Result.Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }

}