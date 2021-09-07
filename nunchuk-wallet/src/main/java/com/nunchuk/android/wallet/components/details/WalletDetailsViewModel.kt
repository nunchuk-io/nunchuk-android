package com.nunchuk.android.wallet.components.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.*
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

internal class WalletDetailsViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val exportCoboWalletUseCase: ExportCoboWalletUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase
) : NunchukViewModel<WalletDetailsState, WalletDetailsEvent>() {

    lateinit var walletId: String

    override val initialState = WalletDetailsState()

    fun init(walletId: String) {
        this.walletId = walletId
    }

    fun syncData() {
        getWalletDetails()
        getTransactionHistory()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .onStart { event(Loading(true)) }
                .flowOn(Dispatchers.IO)
                .catch { event(WalletDetailsError(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .onCompletion { event(Loading(false)) }
                .collect { updateState { copy(wallet = it) } }
        }
    }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            getTransactionHistoryUseCase.execute(walletId)
                .flowOn(Dispatchers.IO)
                .catch { event(WalletDetailsError(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .onCompletion { event(Loading(false)) }
                .collect { onRetrievedTransactionHistory(it) }
        }
    }

    private fun onRetrievedTransactionHistory(result: List<Transaction>) {
        updateState { copy(transactions = result) }
        if (result.isEmpty()) {
            //FIXME
            //getUnusedAddresses()
        }
    }

    private fun getUnusedAddresses() {
        viewModelScope.launch {
            addressesUseCase.execute(walletId = walletId)
                .catch { generateNewAddress() }
                .collect { onRetrieveUnusedAddress(it) }
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
            newAddressUseCase.execute(walletId = walletId)
                .catch { UpdateUnusedAddress("") }
                .collect { UpdateUnusedAddress(it) }
        }
    }

    fun handleSendMoneyEvent() {
        event(SendMoneyEvent(getState().wallet.balance))
    }

    fun handleBackupWallet() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute("${walletId}_descriptor")) {
                is Success -> backupWallet(walletId, event.data)
                is Error -> showError(event)
            }
        }
        getState().wallet.description
    }

    private fun backupWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, ExportFormat.DESCRIPTOR)) {
                is Success -> event(BackupWalletDescriptorEvent(File(filePath).readText(Charsets.UTF_8)))
                is Error -> showError(event)
            }
        }
    }

    fun handleUploadWallet() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute(walletId)) {
                is Success -> uploadWallet(walletId, event.data)
                is Error -> showError(event)
            }
        }
    }

    fun handleExportWalletQR() {
        viewModelScope.launch {
            when (val event = exportCoboWalletUseCase.execute(walletId)) {
                is Success -> event(OpenDynamicQRScreen(event.data))
                is Error -> showError(event)
            }
        }
    }

    private fun uploadWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath)) {
                is Success -> event(UploadWalletConfigEvent(filePath))
                is Error -> showError(event)
            }
        }
    }

    private fun showError(event: Error) {
        WalletDetailsError(event.exception.messageOrUnknownError())
    }

    fun handleDeleteWallet() {
        viewModelScope.launch {
            when (val event = deleteWalletUseCase.execute(walletId)) {
                is Success -> event(DeleteWalletSuccess)
                is Error -> showError(event)
            }
        }
    }

}