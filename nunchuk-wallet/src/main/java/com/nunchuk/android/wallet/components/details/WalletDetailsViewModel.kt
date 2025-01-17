/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.components.details

import android.util.LruCache
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetListMessageFreeGroupWalletUseCase
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.listener.TransactionListener
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetTransactionHistoryUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.usecase.SetSelectedWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.membership.SyncTransactionUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.ImportPSBTSuccess
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.Loading
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.PaginationTransactions
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.SendMoneyEvent
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.UpdateUnusedAddress
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.WalletDetailsError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.room.model.Membership
import javax.inject.Inject

@HiltViewModel
internal class WalletDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWalletUseCase: GetWalletUseCase,
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val sessionHolder: SessionHolder,
    private val accountManager: AccountManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val selectedWalletUseCase: SetSelectedWalletUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val syncTransactionUseCase: SyncTransactionUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val pushEventManager: PushEventManager,
    private val serverTransactionCache: LruCache<String, ServerTransaction>,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val getGroupUseCase: GetGroupUseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val getListMessageFreeGroupWalletUseCase: GetListMessageFreeGroupWalletUseCase,
    ) : NunchukViewModel<WalletDetailsState, WalletDetailsEvent>() {
    private val args: WalletDetailsFragmentArgs =
        WalletDetailsFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val transactions = mutableListOf<Transaction>()

    override val initialState = WalletDetailsState()

    private var syncWalletJob: Job? = null

    init {
        viewModelScope.launch {
            TransactionListener.transactionUpdateFlow.debounce(1000L).collect {
                if (it.walletId == args.walletId) {
                    syncData()
                    if (getState().isHasCoin.not()) {
                        getCoins()
                    }
                }
            }
        }
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect {
                    updateState {
                        copy(
                            hideWalletDetailLocal = it.getOrNull()?.hideWalletDetail == true || getState().role == AssistedWalletRole.FACILITATOR_ADMIN
                        )
                    }
                }
        }
        viewModelScope.launch {
            selectedWalletUseCase(args.walletId)
        }
        syncServerTransaction()
        getCoins()
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                if ((event is PushEvent.TransactionCancelled && event.walletId == args.walletId)
                    || (event is PushEvent.ServerTransactionEvent && event.walletId == args.walletId)
                    || (event is PushEvent.WalletChanged && event.walletId == args.walletId)
                ) {
                    syncData()
                }
            }
        }
        viewModelScope.launch {
            val groupId = assistedWalletManager.getGroupId(args.walletId) ?: return@launch
            getGroupUseCase(GetGroupUseCase.Params(groupId))
                .collect {
                    updateState {
                        val role = byzantineGroupUtils.getCurrentUserRole(it.getOrNull()).toRole
                        copy(
                            role = role,
                            hideWalletDetailLocal = getState().hideWalletDetailLocal || role == AssistedWalletRole.FACILITATOR_ADMIN
                        )
                    }
                }
        }
        viewModelScope.launch {
            getListMessageFreeGroupWalletUseCase(
                GetListMessageFreeGroupWalletUseCase.Param(
                    walletId = args.walletId,
                )
            ).onSuccess {
                updateState {
                    copy(
                        groupChatMessages = it.takeLast(2),
                    )
                }
            }
        }
    }

    private fun getCoins() {
        viewModelScope.launch {
            getAllCoinUseCase(args.walletId).onSuccess { coins ->
                updateState { copy(isHasCoin = coins.isNotEmpty()) }
            }
        }
    }

    private suspend fun syncTransactionFromServer() {
        val result = syncTransactionUseCase(
            SyncTransactionUseCase.Params(
                assistedWalletManager.getGroupId(args.walletId), args.walletId
            )
        )
        if (result.isSuccess) {
            getTransactionHistory()
        }
    }

    fun syncServerTransaction() {
        viewModelScope.launch {
            if (assistedWalletManager.isActiveAssistedWallet(args.walletId)) {
                syncTransactionFromServer()
            }
        }
    }

    // well, don't do this, you know why
    fun getRoomWallet() = getState().walletExtended.roomWallet

    fun syncData(loadingSilent: Boolean = false) {
        getWalletDetails(loadingSilent = loadingSilent)
    }

    fun getWalletDetails(shouldRefreshTransaction: Boolean = true, loadingSilent: Boolean = false) {
        syncWalletJob?.cancel()
        syncWalletJob = viewModelScope.launch {
            getWalletUseCase.execute(args.walletId)
                .onStart { if (loadingSilent.not()) event(Loading(true)) }
                .flowOn(IO)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect {
                    updateState {
                        copy(
                            walletExtended = it,
                            isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId),
                            walletStatus = assistedWalletManager.getBriefWallet(args.walletId)?.status,
                            groupId = assistedWalletManager.getGroupId(args.walletId)
                        )
                    }
                    if (shouldRefreshTransaction) {
                        checkUserInRoom(it.roomWallet)
                        getTransactionHistory()
                    } else {
                        event(Loading(false))
                    }
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
            getTransactionHistoryUseCase.execute(args.walletId).flowOn(IO)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }.flowOn(Main)
                .collect {
                    transactions.clear()
                    transactions.addAll(
                        it.sortedWith(
                            compareBy(Transaction::status).thenByDescending(
                                Transaction::blockTime
                            )
                        )
                    )
                    onRetrievedTransactionHistory()
                }
        }
    }

    fun paginateTransactions() =
        Pager(config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                TransactionPagingSource(
                    transactions = transactions.toList(),
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId),
                    serverTransactionCache = serverTransactionCache,
                )
            }).flow.flowOn(ioDispatcher)

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
            addressesUseCase.execute(walletId = args.walletId).flowOn(IO)
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
            newAddressUseCase.execute(walletId = args.walletId).flowOn(IO)
                .onException { event(UpdateUnusedAddress("")) }
                .collect { event(UpdateUnusedAddress(it)) }
        }
    }

    fun handleSendMoneyEvent() {
        event(SendMoneyEvent(getState().walletExtended))
    }

    fun handleImportPSBT(filePath: String) {
        viewModelScope.launch {
            importTransactionUseCase(
                ImportTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(args.walletId),
                    walletId = args.walletId,
                    filePath = filePath,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
                )
            ).onSuccess {
                event(ImportPSBTSuccess)
                getTransactionHistory()
            }.onFailure {
                event(WalletDetailsError(it.readableMessage()))
            }
        }
    }

    fun setForceRefreshWalletProcessing(isProcessing: Boolean) {
        updateState { copy(isForceRefreshProcessing = isProcessing) }
    }

    fun updateHideWalletDetailLocal() {
        val hideWalletDetailLocal = getState().hideWalletDetailLocal.not()
        updateState { copy(hideWalletDetailLocal = hideWalletDetailLocal) }
    }

    val isForceRefreshProcessing: Boolean
        get() = getState().isForceRefreshProcessing

    val isHideWalletDetailLocal: Boolean
        get() = getState().hideWalletDetailLocal || getState().role == AssistedWalletRole.FACILITATOR_ADMIN

    val isLeaveRoom: Boolean
        get() = getState().isLeaveRoom

    val isAssistedWallet: Boolean
        get() = getState().isAssistedWallet

    val groupId: String?
        get() = getState().groupId

    val isLockedAssistedWallet: Boolean
        get() = getState().walletStatus == WalletStatus.LOCKED.name

    fun isInactiveAssistedWallet() = assistedWalletManager.isInactiveAssistedWallet(args.walletId)

    fun isFacilitatorAdmin() = getState().role == AssistedWalletRole.FACILITATOR_ADMIN

    fun isEmptyTransaction() = transactions.isEmpty()

    fun getWallet() = getState().walletExtended.wallet

    suspend fun hasSigner(signer: SingleSigner) = hasSignerUseCase(signer)
}