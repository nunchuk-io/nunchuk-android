package com.nunchuk.android.wallet.components.details

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.room.model.Membership
import javax.inject.Inject

@HiltViewModel
internal class WalletDetailsViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val sessionHolder: SessionHolder,
    private val accountManager: AccountManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<WalletDetailsState, WalletDetailsEvent>() {

    lateinit var walletId: String

    private var transactions: List<Transaction> = ArrayList()

    override val initialState = WalletDetailsState()

    fun init(walletId: String, shouldReloadPendingTx: Boolean) {
        this.walletId = walletId
        if (shouldReloadPendingTx) {
            handleLoadPendingTx()
        }
    }

    // well, don't do this, you know why
    fun getRoomWallet() = getState().walletExtended.roomWallet

    fun syncData() {
        transactions = ArrayList()
        getWalletDetails()
    }

    private fun handleLoadPendingTx() {
        viewModelScope.launch {
            delay(TIMEOUT_TO_RELOAD)
            syncData()
        }
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId).onStart { event(Loading(true)) }.flowOn(IO)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }.flowOn(Main)
                .collect {
                    updateState { copy(walletExtended = it) }
                    checkUserInRoom(it.roomWallet)
                    getTransactionHistory()
                }
        }
    }

    private fun checkUserInRoom(roomWallet: RoomWallet?) {
        roomWallet ?: return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                sessionHolder.getSafeActiveSession()?.let {
                    val account = accountManager.getAccount()
                    it.roomService().getRoom(roomWallet.roomId)?.membershipService()
                        ?.getRoomMember(account.chatId)
                }
            }
            if (result == null || result.membership == Membership.LEAVE) {
                updateState {
                    copy(
                        isLeaveRoom = true
                    )
                }
            }
        }
    }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            getTransactionHistoryUseCase.execute(walletId).flowOn(IO)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }.flowOn(Main)
                .collect {
                    transactions =
                        it.sortedWith(compareBy(Transaction::status).thenByDescending(Transaction::blockTime))
                    onRetrievedTransactionHistory()
                }
        }
    }

    fun paginateTransactions() =
        Pager(config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { TransactionPagingSource(transactions) }).flow.cachedIn(
            viewModelScope
        ).flowOn(IO)

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
            addressesUseCase.execute(walletId = walletId).flowOn(IO)
                .onException { generateNewAddress() }.flowOn(Main)
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
            newAddressUseCase.execute(walletId = walletId).flowOn(IO)
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

    private fun exportWalletToFile(walletId: String, filePath: String, format: ExportFormat) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, format)) {
                is Success -> event(UploadWalletConfigEvent(filePath))
                is Error -> showError(event)
            }
        }
    }

    private fun showError(event: Error) {
        WalletDetailsError(event.exception.messageOrUnknownError())
    }

    fun handleImportPSBT(filePath: String) {
        viewModelScope.launch {
            importTransactionUseCase.execute(walletId, filePath).flowOn(IO)
                .onException { event(WalletDetailsError(it.readableMessage())) }.flowOn(Main)
                .collect {
                    event(ImportPSBTSuccess)
                    getTransactionHistory()
                }
        }
    }

    val isLeaveRoom: Boolean
        get() = getState().isLeaveRoom

    companion object {
        private const val TIMEOUT_TO_RELOAD = 5000L
    }
}