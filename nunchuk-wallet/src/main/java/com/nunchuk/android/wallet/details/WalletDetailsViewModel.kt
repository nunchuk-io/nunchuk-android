package com.nunchuk.android.wallet.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.usecase.GetTransactionHistoryUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.wallet.details.WalletDetailsEvent.WalletDetailsError
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletDetailsViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase
) : NunchukViewModel<WalletDetailsState, WalletDetailsEvent>() {

    override val initialState = WalletDetailsState()

    lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
        getWalletDetails()
        getTransactionHistory()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            when (val result = getWalletUseCase.execute(walletId)) {
                is Success -> updateState { copy(wallet = result.data) }
                is Error -> event(WalletDetailsError(result.exception.message.orUnknownError()))
            }
        }
    }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            when (val result = getTransactionHistoryUseCase.execute(walletId)) {
                is Success -> updateState { copy(transactions = result.data) }
                is Error -> event(WalletDetailsError(result.exception.message.orUnknownError()))
            }
        }
    }

}