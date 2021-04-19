package com.nunchuk.android.wallet.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletInfoViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase
) : NunchukViewModel<Wallet, WalletInfoEvent>() {

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
                is Result.Error -> {
                }
            }
        }
    }

    fun handleEditCompleteEvent(walletName: String) {
        viewModelScope.launch {
            when (updateWalletUseCase.execute(getState().copy(name = walletName))) {
                is Result.Success -> {
                    updateState { copy(name = walletName) }
                }
                is Result.Error -> {
                }
            }
        }
    }

}