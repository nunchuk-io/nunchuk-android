package com.nunchuk.android.wallet.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.usecase.*
import com.nunchuk.android.wallet.details.WalletDetailsEvent.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletDetailsViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val exportCoboWalletUseCase: ExportCoboWalletUseCase,
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
                is Success -> onRetrievedTransactionHistory(result.data)
                is Error -> event(WalletDetailsError(result.exception.message.orUnknownError()))
            }
        }
    }

    private fun onRetrievedTransactionHistory(result: List<Transaction>) {
        updateState { copy(transactions = result) }
        if (result.isEmpty()) {
            getUnusedAddresses()
        }
    }

    private fun getUnusedAddresses() {
        viewModelScope.launch {
            when (val result = addressesUseCase.execute(walletId = walletId, used = false, internal = false)) {
                is Success -> onRetrieveUnusedAddress(result.data)
                is Error -> {
                    generateNewAddress()
                }
            }
        }
    }

    private fun onRetrieveUnusedAddress(addresses: List<String>) {
        if (addresses.isEmpty()) {
            generateNewAddress()
        } else {
            event(UpdateUnusedAddress(addresses.first()))
        }
    }

    private fun generateNewAddress() {
        viewModelScope.launch {
            when (val result = newAddressUseCase.execute(walletId = walletId)) {
                is Success -> event(UpdateUnusedAddress(result.data))
                is Error -> UpdateUnusedAddress("")
            }
        }
    }

    fun handleSendMoneyEvent() {
        event(SendMoneyEvent(getState().wallet.balance))
    }

    fun handleBackupWallet() {
        viewModelScope.launch {
            when (val event = exportCoboWalletUseCase.execute(walletId)) {
                is Success -> {
                    event(OpenDynamicQRScreen(event.data))
                }
                is Error -> {
                }
            }
        }
    }

}