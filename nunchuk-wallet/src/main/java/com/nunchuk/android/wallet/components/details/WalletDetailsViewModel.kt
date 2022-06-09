package com.nunchuk.android.wallet.components.details

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletDetailsViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val exportKeystoneWalletUseCase: ExportKeystoneWalletUseCase,
    private val exportPassportWalletUseCase: ExportPassportWalletUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val importTransactionUseCase: ImportTransactionUseCase,
) : NunchukViewModel<WalletDetailsState, WalletDetailsEvent>() {

    lateinit var walletId: String

    private var transactions: List<Transaction> = ArrayList()

    override val initialState = WalletDetailsState()

    fun init(walletId: String) {
        this.walletId = walletId
    }

    // well, don't do this, you know why
    fun getRoomWallet() = getState().walletExtended.roomWallet

    fun syncData() {
        transactions = ArrayList()
        getWalletDetails()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .onStart { event(Loading(true)) }
                .flowOn(IO)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect {
                    updateState { copy(walletExtended = it) }
                    getTransactionHistory()
                }
        }
    }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            getTransactionHistoryUseCase.execute(walletId)
                .flowOn(IO)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect {
                    transactions = it.sortedWith(compareBy(Transaction::status).thenByDescending(Transaction::blockTime))
                    onRetrievedTransactionHistory()
                }
        }
    }

    fun paginateTransactions() = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { TransactionPagingSource(transactions) }
    ).flow.cachedIn(viewModelScope).flowOn(IO)

    private fun onRetrievedTransactionHistory() {
        if (transactions.isEmpty()) {
            getUnusedAddresses()
            event(PaginationTransactions(false))
        } else {
            event(PaginationTransactions(true))
        }
    }

    private fun getUnusedAddresses() {
        viewModelScope.launch {
            addressesUseCase.execute(walletId = walletId)
                .flowOn(IO)
                .onException { generateNewAddress() }
                .flowOn(Main)
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
                .flowOn(IO)
                .onException { event(UpdateUnusedAddress("")) }
                .collect { event(UpdateUnusedAddress(it)) }
        }
    }

    fun handleSendMoneyEvent() {
        event(SendMoneyEvent(getState().walletExtended))
    }

    fun handleExportBSMS() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute("$walletId.bsms")) {
                is Success -> exportWalletToFile(walletId, event.data, ExportFormat.BSMS)
                is Error -> showError(event)
            }
        }
    }

    fun handleExportColdcard() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute(walletId + "_coldcard_export.txt")) {
                is Success -> exportWalletToFile(walletId, event.data, ExportFormat.COLDCARD)
                is Error -> showError(event)
            }
        }
    }

    private fun exportWalletToFile(walletId: String, filePath: String, format: ExportFormat) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, format)) {
                is Success -> event(UploadWalletConfigEvent(filePath))
                is Error -> showError(event)
            }
        }
    }

    fun handleExportWalletQR() {
        viewModelScope.launch {
            exportKeystoneWalletUseCase.execute(walletId)
                .flowOn(IO)
                .onException { showError(it) }
                .flowOn(Main)
                .collect { event(OpenDynamicQRScreen(it)) }
        }
    }

    fun handleExportPassport() {
        viewModelScope.launch {
            exportPassportWalletUseCase.execute(walletId)
                .flowOn(IO)
                .onException { showError(it) }
                .flowOn(Main)
                .collect { event(OpenDynamicQRScreen(it)) }
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

    fun handleImportPSBT(filePath: String) {
        viewModelScope.launch {
            importTransactionUseCase.execute(walletId, filePath)
                .flowOn(IO)
                .onException { event(WalletDetailsError(it.readableMessage())) }
                .flowOn(Main)
                .collect {
                    event(ImportPSBTSuccess)
                    getTransactionHistory()
                }
        }
    }
}