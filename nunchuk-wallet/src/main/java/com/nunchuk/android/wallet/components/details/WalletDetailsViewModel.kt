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
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

internal class WalletDetailsViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val exportKeystoneWalletUseCase: ExportKeystoneWalletUseCase,
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
                .collect {
                    updateState { copy(wallet = it) }
                    event(Loading(false))
                }
        }
    }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            getTransactionHistoryUseCase.execute(walletId)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }
                .collect { onRetrievedTransactionHistory(it) }
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
            addressesUseCase.execute(walletId = walletId)
                .onException { generateNewAddress() }
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
                .onException { event(UpdateUnusedAddress("")) }
                .collect { event(UpdateUnusedAddress(it)) }
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
            exportKeystoneWalletUseCase.execute(walletId)
                .flowOn(Dispatchers.IO)
                .onException { showError(it) }
                .flowOn(Dispatchers.Main)
                .collect { event(OpenDynamicQRScreen(it)) }
        }
    }

    private fun uploadWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, ExportFormat.BSMS)) {
                is Success -> event(UploadWalletConfigEvent(filePath))
                is Error -> showError(event)
            }
        }
    }

    private fun showError(event: Error) {
        WalletDetailsError(event.exception.messageOrUnknownError())
    }

    private fun showError(t: Throwable) {
        event(WalletDetailsError(t.message.orUnknownError()))
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