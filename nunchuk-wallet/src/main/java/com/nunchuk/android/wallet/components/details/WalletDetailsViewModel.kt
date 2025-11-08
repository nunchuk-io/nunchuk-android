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
import com.nunchuk.android.core.domain.GetGroupDeviceUIDUseCase
import com.nunchuk.android.core.domain.GetListMessageFreeGroupWalletUseCase
import com.nunchuk.android.core.domain.GetWalletBannerStateUseCase
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.domain.membership.IsClaimWalletUseCase
import com.nunchuk.android.core.domain.wallet.GetWalletBsmsUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.getNearestTimeLock
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.listener.GroupMessageListener
import com.nunchuk.android.listener.GroupReplaceListener
import com.nunchuk.android.listener.TransactionListener
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetGlobalGroupWalletConfigUseCase
import com.nunchuk.android.usecase.GetGroupWalletConfigUseCase
import com.nunchuk.android.usecase.GetGroupWalletMessageUnreadCountUseCase
import com.nunchuk.android.usecase.GetTimelockedUntilUseCase
import com.nunchuk.android.usecase.GetTransactionHistoryUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.SetGroupWalletLastReadMessageUseCase
import com.nunchuk.android.usecase.SetSelectedWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.free.groupwallet.AcceptReplaceGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.DeclineReplaceGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetBackUpBannerWalletIdsUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetDeprecatedGroupWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetReplaceGroupsUseCase
import com.nunchuk.android.usecase.free.groupwallet.SetBackUpBannerWalletIdsUseCase
import com.nunchuk.android.usecase.membership.SyncTransactionUseCase
import com.nunchuk.android.usecase.miniscript.GetSpendableNowAmountUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.GroupChatManager
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.ImportPSBTSuccess
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.Loading
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.PaginationTransactions
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.SaveLocalFile
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.SendMoneyEvent
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.ShareBSMS
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.UpdateUnusedAddress
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.WalletDetailsError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.room.model.Membership
import timber.log.Timber
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
    private val timelockTransactionCache: LruCache<String, Long>,
    private val walletLockedBase: LruCache<String, MiniscriptTimelockBased>,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val getGroupUseCase: GetGroupUseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val getListMessageFreeGroupWalletUseCase: GetListMessageFreeGroupWalletUseCase,
    private val groupChatManager: GroupChatManager,
    private val getGroupWalletsUseCase: GetGroupWalletsUseCase,
    private val getGlobalGroupWalletConfigUseCase: GetGlobalGroupWalletConfigUseCase,
    private val getGroupWalletConfigUseCase: GetGroupWalletConfigUseCase,
    private val setGroupWalletLastReadMessageUseCase: SetGroupWalletLastReadMessageUseCase,
    private val getGroupWalletMessageUnreadCountUseCase: GetGroupWalletMessageUnreadCountUseCase,
    private val getGroupDeviceUIDUseCase: GetGroupDeviceUIDUseCase,
    private val getReplaceGroupsUseCase: GetReplaceGroupsUseCase,
    private val acceptReplaceGroupUseCase: AcceptReplaceGroupUseCase,
    private val declineReplaceGroupUseCase: DeclineReplaceGroupUseCase,
    private val getDeprecatedGroupWalletsUseCase: GetDeprecatedGroupWalletsUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    private val getBackUpBannerWalletIdsUseCase: GetBackUpBannerWalletIdsUseCase,
    private val setBackUpBannerWalletIdsUseCase: SetBackUpBannerWalletIdsUseCase,
    private val getWalletBannerStateUseCase: GetWalletBannerStateUseCase,
    private val getChainTipUseCase: GetChainTipUseCase,
    private val getSpendableNowAmountUseCase: GetSpendableNowAmountUseCase,
    private val getTimelockedUntilUseCase: GetTimelockedUntilUseCase,
    private val isClaimWalletUseCase: IsClaimWalletUseCase,
    private val getWalletBsmsUseCase: GetWalletBsmsUseCase,
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
                    page = 0
                )
            ).onSuccess {
                updateState {
                    copy(
                        groupChatMessages = it,
                    )
                }
            }
        }
        viewModelScope.launch {
            val uid = getGroupDeviceUIDUseCase(Unit).getOrNull()
            updateState {
                copy(uid = uid.orEmpty())
            }
            getGroupWalletsUseCase(Unit).onSuccess { wallets ->
                val isFreeGroupWallet = wallets.any { it.id == args.walletId }
                if (isFreeGroupWallet) {
                    groupChatManager.init(args.walletId)
                }
                updateState { copy(isFreeGroupWallet = isFreeGroupWallet) }
            }
        }
        viewModelScope.launch {
            GroupMessageListener.getMessageFlow().collect { message ->
                if (message.walletId == args.walletId) {
                    val groupChatMessages = getState().groupChatMessages.toMutableList()
                    if (groupChatMessages.contains(message)) return@collect
                    groupChatMessages.add(0, message)
                    updateState {
                        copy(
                            groupChatMessages = groupChatMessages
                        )
                    }
                    if (message.sender == getState().uid) {
                        Timber.tag("group-wallet-chat").e("message from me")
                        setLastReadMessage(message.id)
                    } else {
                        getGroupWalletMessageUnreadCount()
                    }
                }
            }
        }
        viewModelScope.launch {
            getBackUpBannerWalletIdsUseCase(Unit)
                .collect {
                    updateState {
                        copy(
                            isNeedBackUpGroupWallet = it.getOrNull()
                                ?.contains(args.walletId) == false
                        )
                    }
                }

        }
        viewModelScope.launch {
            while (isActive) {
                if (getWallet().miniscript.isNotEmpty() && getState().nearestTimeLock != null) {
                    getChainTipUseCase(Unit).onSuccess { blockHeight ->
                        updateState {
                            copy(
                                currentBlock = blockHeight
                            )
                        }
                    }
                }
                delay(60000)
            }
        }
        getGroupWalletMessageUnreadCount()
        listenGroupWalletReplace()
        getWalletBannerState()
        checkClaimWallet()
    }

    private fun checkClaimWallet() {
        viewModelScope.launch {
            isClaimWalletUseCase(args.walletId).onSuccess { isClaimWallet ->
                updateState {
                    copy(isClaimWallet = isClaimWallet)
                }
            }
        }
    }

    fun checkDeprecatedGroupWallet() {
        viewModelScope.launch {
            getDeprecatedGroupWalletsUseCase(Unit).onSuccess { deprecatedWallets ->
                val isDeprecated = deprecatedWallets.any { it == args.walletId }
                getReplaceGroupSandbox()
                updateState { copy(isDeprecatedGroupWallet = isDeprecated) }
            }
        }
    }

    private fun getReplaceGroupSandbox() {
        viewModelScope.launch {
            getReplaceGroupsUseCase(args.walletId).onSuccess { replaceGroups ->
                updateState {
                    copy(
                        replaceGroups = replaceGroups
                    )
                }
            }
        }
    }

    private fun listenGroupWalletReplace() {
        viewModelScope.launch {
            GroupReplaceListener.groupReplaceFlow.collect { replaceGroup ->
                if (replaceGroup.walletId == args.walletId) {
                    getReplaceGroupSandbox()
                }
            }
        }
    }

    fun getGroupWalletMessageUnreadCount() {
        viewModelScope.launch {
            getGroupWalletMessageUnreadCountUseCase(
                GetGroupWalletMessageUnreadCountUseCase.Params(
                    walletId = args.walletId
                )
            ).onSuccess {
                updateState {
                    copy(
                        unreadMessagesCount = it
                    )
                }
            }
        }
    }

    private fun getFreeGroupWalletConfig() {
        viewModelScope.launch {
            val globalConfigResult = async {
                getGlobalGroupWalletConfigUseCase(getWallet().addressType)
            }
            val walletConfigResult = async {
                getGroupWalletConfigUseCase(args.walletId)
            }
            val globalConfig = globalConfigResult.await()
            val walletConfig = walletConfigResult.await()
            val historyPeriods =
                globalConfig.getOrNull()?.retentionDaysOptions?.toList()?.sortedDescending()?.map {
                    HistoryPeriod(
                        id = it.toString(),
                        durationInMillis = it.toLong(),
                        displayName = if (it > 1) "$it days" else "$it day",
                        enabled = true
                    )
                }.orEmpty()

            if (historyPeriods.isEmpty()) return@launch

            updateState {
                copy(
                    historyPeriods = historyPeriods,
                    selectedHistoryPeriod = historyPeriods.firstOrNull { it.id == walletConfig.getOrNull()?.chatRetentionDays.toString() }
                        ?: historyPeriods.first()
                )
            }
        }
    }

    private fun setLastReadMessage(messageId: String) {
        viewModelScope.launch {
            setGroupWalletLastReadMessageUseCase(
                SetGroupWalletLastReadMessageUseCase.Params(
                    walletId = args.walletId,
                    lastReadMessageId = messageId
                )
            )
            updateState {
                copy(unreadMessagesCount = 0)
            }
        }
    }

    fun updateGroupChatHistoryPeriod(period: HistoryPeriod) {
        updateState {
            copy(
                selectedHistoryPeriod = period
            )
        }
    }

    private fun getCoins() {
        viewModelScope.launch {
            getAllCoinUseCase(args.walletId).onSuccess { coins ->
                val currentBlockHeight = getChainTipUseCase(Unit).getOrDefault(0)
                var nearestTimeLock = Long.MAX_VALUE
                coins.forEach { coin ->
                    coin.getNearestTimeLock(currentBlockHeight)?.let { time ->
                        nearestTimeLock = minOf(nearestTimeLock, time)
                    }
                }
                updateState {
                    copy(
                        isHasCoin = coins.isNotEmpty(),
                        nearestTimeLock = coins.firstOrNull()
                            ?.takeIf { nearestTimeLock != Long.MAX_VALUE }
                            ?.let { it.lockBased to nearestTimeLock },
                    )
                }
            }
            getSpendableNowAmountUseCase(args.walletId).onSuccess { amount ->
                updateState {
                    copy(
                        noTimelockCoinsAmount = amount
                    )
                }
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
            val currentBlock = getChainTipUseCase(Unit).getOrDefault(0)
            getWalletUseCase.execute(args.walletId)
                .onStart { if (loadingSilent.not()) event(Loading(true)) }
                .flowOn(IO)
                .onException { event(WalletDetailsError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect {
                    val brief = assistedWalletManager.getBriefWallet(args.walletId)
                    updateState {
                        copy(
                            walletExtended = it,
                            isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId),
                            walletStatus = brief?.status,
                            groupId = assistedWalletManager.getGroupId(args.walletId),
                            currentBlock = currentBlock
                        )
                    }
                    if (shouldRefreshTransaction) {
                        checkUserInRoom(it.roomWallet)
                        getTransactionHistory()
                    } else {
                        event(Loading(false))
                    }
                    getFreeGroupWalletConfig()
                    getWalletBannerState()
                    checkClaimWallet()
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
                    updateLockedTimeCache()
                    onRetrievedTransactionHistory()
                }
        }
    }

    /**
     * Updates the locked time cache for all transactions in the wallet
     */
    private suspend fun updateLockedTimeCache() {
        val wallet = getWallet()
        if (wallet.miniscript.isNotEmpty()) {
            transactions.filter { tx -> !tx.status.hadBroadcast() && timelockTransactionCache[tx.txId] == null }
                .forEach { transaction ->
                    getTimelockedUntilUseCase(
                        GetTimelockedUntilUseCase.Params(
                            walletId = args.walletId,
                            txId = transaction.txId
                        )
                    ).onSuccess { (lockedTime, lockedBaseType) ->
                        timelockTransactionCache.put(transaction.txId, lockedTime)
                        if (lockedBaseType != MiniscriptTimelockBased.NONE) {
                            walletLockedBase.put(args.walletId, lockedBaseType)
                        }
                    }
                }
        }
    }

    fun paginateTransactions() =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                TransactionPagingSource(
                    transactions = transactions.toList(),
                    brief = assistedWalletManager.getBriefWallet(args.walletId),
                    serverTransactionCache = serverTransactionCache,
                    lockedTimeTransactionCache = timelockTransactionCache,
                    lockedBase = walletLockedBase[args.walletId] ?: MiniscriptTimelockBased.NONE,
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
        get() = getState().walletStatus == WalletStatus.LOCKED.name && !getState().isFreeGroupWallet

    fun isInactiveAssistedWallet() = assistedWalletManager.isInactiveAssistedWallet(args.walletId)

    fun isFacilitatorAdmin() = getState().role == AssistedWalletRole.FACILITATOR_ADMIN

    fun isEmptyTransaction() = transactions.isEmpty()

    fun getWallet() = getState().walletExtended.wallet

    fun isFreeGroupWallet() = getState().isFreeGroupWallet

    suspend fun hasSigner(signer: SingleSigner) = hasSignerUseCase(signer)

    fun sendMessage(message: String) = viewModelScope.launch {
        groupChatManager.sendMessage(message, args.walletId) {
            event(WalletDetailsError(it.message.orUnknownError()))
        }
    }

    fun acceptOrDenyReplaceGroup(groupId: String, accept: Boolean) {
        viewModelScope.launch {
            if (accept) {
                acceptReplaceGroupUseCase(
                    AcceptReplaceGroupUseCase.Params(
                        walletId = args.walletId,
                        groupId = groupId
                    )
                )
            } else {
                declineReplaceGroupUseCase(
                    DeclineReplaceGroupUseCase.Params(
                        walletId = args.walletId,
                        groupId = groupId
                    )
                )
            }.onSuccess {
                if (accept) {
                    setEvent(WalletDetailsEvent.OpenSetupGroupWallet(groupId))
                    updateState { copy(replaceGroups = mutableMapOf(groupId to true)) }
                } else {
                    updateState { copy(replaceGroups = emptyMap()) }
                }
            }.onFailure {
                setEvent(WalletDetailsError(it.readableMessage()))
            }
        }
    }

    fun handleExportBSMS(isShareFile: Boolean) {
        viewModelScope.launch {
            val walletId = getWallet().id
            when (val event = createShareFileUseCase.execute("${walletId}.bsms")) {
                is Success -> exportWalletToFile(walletId, event.data, isShareFile)
                is Result.Error -> setEvent(WalletDetailsError(event.exception.messageOrUnknownError()))
            }
        }
    }

    private fun exportWalletToFile(walletId: String, filePath: String, isShareFile: Boolean) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, ExportFormat.BSMS)) {
                is Success -> {
                    if (isShareFile) {
                        setEvent(ShareBSMS(filePath))
                        markGroupWalletAsBackedUp()
                    } else {
                        saveBSMSToLocal(filePath)
                    }
                }

                is Result.Error -> setEvent(WalletDetailsError(event.exception.messageOrUnknownError()))
            }
        }
    }

    fun saveBSMSToLocal(filePath: String) {
        viewModelScope.launch {
            val result = saveLocalFileUseCase(
                SaveLocalFileUseCase.Params(
                    fileName = "${getWallet().id}.bsms",
                    filePath = filePath
                )
            )
            setEvent(SaveLocalFile(result.isSuccess))
            if (result.isSuccess) {
                markGroupWalletAsBackedUp()
            }
        }
    }

    fun markGroupWalletAsBackedUp() {
        viewModelScope.launch {
            updateState { copy(isNeedBackUpGroupWallet = false) }
            setBackUpBannerWalletIdsUseCase(getWallet().id)
        }
    }

    fun getChatBarState() = getState().chatBarState

    fun setChatBarState(state: ChatBarState) {
        updateState {
            copy(chatBarState = state)
        }
    }

    /**
     * Retrieve the wallet banner state and store it in the state
     */
    private fun getWalletBannerState() {
        viewModelScope.launch {
            getWalletBannerStateUseCase(args.walletId).onSuccess { bannerState ->
                updateState {
                    copy(bannerState = bannerState)
                }
            }.onFailure {
                // Silently handle failure - banner state is optional
                updateState {
                    copy(bannerState = null)
                }
            }
        }
    }

    suspend fun getWalletBsms(): String? = getWalletBsmsUseCase(getWallet()).getOrNull()
}