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

package com.nunchuk.android.transaction.components.details

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.util.LruCache
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.ExportPsbtToMk4UseCase
import com.nunchuk.android.core.domain.GetRawTransactionUseCase
import com.nunchuk.android.core.domain.ImportTransactionFromMk4UseCase
import com.nunchuk.android.core.domain.SignRoomTransactionByTapSignerUseCase
import com.nunchuk.android.core.domain.SignTransactionByTapSignerUseCase
import com.nunchuk.android.core.domain.membership.CancelScheduleBroadcastTransactionUseCase
import com.nunchuk.android.core.domain.membership.RequestSignatureTransactionUseCase
import com.nunchuk.android.core.domain.utils.ParseSignerStringUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.miniscript.isPreImageNode
import com.nunchuk.android.core.network.ApiErrorCode
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.MusigKeyPrefix
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.isNoInternetException
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.isPendingConfirm
import com.nunchuk.android.core.util.isPendingSignatures
import com.nunchuk.android.core.util.isRejected
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.isValueKeySetDisable
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.listener.BlockListener
import com.nunchuk.android.listener.TransactionListener
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinsGroup
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.Device
import com.nunchuk.android.model.KeySetStatus
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.joinKeys
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.BroadcastTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.CancelScheduleBroadcastTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.DeleteTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ExportToFileSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ExportTransactionToMk4Success
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.GetRawTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ImportTransactionFromMk4Success
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ImportTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.LoadingEvent
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.NfcLoadingEvent
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.NoInternetConnection
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.PromptInputPassphrase
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.PromptTransactionOptions
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.SignTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.TransactionDetailsError
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.TransactionError
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.UpdateTransactionMemoFailed
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.UpdateTransactionMemoSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ViewBlockchainExplorer
import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCase
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.WalletTemplate
import com.nunchuk.android.usecase.BroadcastTransactionUseCase
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.DeleteTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCase
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetKeySetStatusUseCase
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.GetTimelockedUntilUseCase
import com.nunchuk.android.usecase.GetTransactionFromNetworkUseCase
import com.nunchuk.android.usecase.GetTransactionUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.usecase.IsPreimageRevealedUseCase
import com.nunchuk.android.usecase.IsScriptNodeSatisfiableUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.usecase.SignTransactionUseCase
import com.nunchuk.android.usecase.UpdateTransactionMemo
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.GetCoinsFromTxInputsUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListLocalUseCase
import com.nunchuk.android.usecase.membership.SignServerTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.BroadcastRoomTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.GetPendingTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.SignRoomTransactionUseCase
import com.nunchuk.android.usecase.transaction.GetTaprootKeySetSelectionUseCase
import com.nunchuk.android.usecase.transaction.GetTransactionSignersUseCase
import com.nunchuk.android.usecase.transaction.ImportPsbtUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.TransactionException
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.retrieveInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

// TODO: Remove TaprootKeySetSelection if tx status is confirmed
@HiltViewModel
internal class TransactionDetailsViewModel @Inject constructor(
    private val getBlockchainExplorerUrlUseCase: GetBlockchainExplorerUrlUseCase,
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val signTransactionUseCase: SignTransactionUseCase,
    private val signRoomTransactionUseCase: SignRoomTransactionUseCase,
    private val broadcastTransactionUseCase: BroadcastTransactionUseCase,
    private val broadcastRoomTransactionUseCase: BroadcastRoomTransactionUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportTransactionUseCase: ExportTransactionUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val signTransactionByTapSignerUseCase: SignTransactionByTapSignerUseCase,
    private val signRoomTransactionByTapSignerUseCase: SignRoomTransactionByTapSignerUseCase,
    private val updateTransactionMemo: UpdateTransactionMemo,
    private val exportPsbtToMk4UseCase: ExportPsbtToMk4UseCase,
    private val importTransactionFromMk4UseCase: ImportTransactionFromMk4UseCase,
    private val getPendingTransactionUseCase: GetPendingTransactionUseCase,
    private val getTransactionFromNetworkUseCase: GetTransactionFromNetworkUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val accountManager: AccountManager,
    private val singleSignerMapper: SingleSignerMapper,
    private val assistedWalletManager: AssistedWalletManager,
    private val pushEventManager: PushEventManager,
    private val signServerTransactionUseCase: SignServerTransactionUseCase,
    private val cancelScheduleBroadcastTransactionUseCase: CancelScheduleBroadcastTransactionUseCase,
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val getRawTransactionUseCase: GetRawTransactionUseCase,
    private val requestSignatureTransactionUseCase: RequestSignatureTransactionUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val getCoinsFromTxInputsUseCase: GetCoinsFromTxInputsUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val application: Application,
    private val importPsbtUseCase: ImportPsbtUseCase,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    private val getTaprootKeySetSelectionUseCase: GetTaprootKeySetSelectionUseCase,
    private val getSavedAddressListLocalUseCase: GetSavedAddressListLocalUseCase,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
    private val parseSignerStringUseCase: ParseSignerStringUseCase,
    private val isScriptNodeSatisfiableUseCase: IsScriptNodeSatisfiableUseCase,
    private val getTimelockedUntilUseCase: GetTimelockedUntilUseCase,
    private val getChainTipUseCase: GetChainTipUseCase,
    private val isPreimageRevealedUseCase: IsPreimageRevealedUseCase,
    private val getKeySetStatusUseCase: GetKeySetStatusUseCase,
    private val getTransactionSignersUseCase: GetTransactionSignersUseCase,
    private val timelockTransactionCache: LruCache<String, Long>,
    private val walletLockedBase: LruCache<String, MiniscriptTimelockBased>,
) : ViewModel() {
    private val _state = MutableStateFlow(TransactionDetailsState())
    val state = _state.asStateFlow()

    private val _minscriptState = MutableStateFlow(TransactionMiniscriptUiState())
    val miniscriptState = _minscriptState.asStateFlow()

    private val _event = MutableSharedFlow<TransactionDetailsEvent>()
    val event = _event.asSharedFlow()

    private val reviewManager by lazy { ReviewManagerFactory.create(application) }
    private var walletId: String = ""
    private var txId: String = ""
    private var initEventId: String = ""
    private var roomId: String = ""
    private var initTransaction: Transaction? = null
    private var isClaimingInheritance: Boolean = false

    private var masterSigners: List<MasterSigner> = emptyList()

    private var contacts: List<Contact> = emptyList()

    private var initNumberOfSignedKey = INVALID_NUMBER_OF_SIGNED

    private var reloadTransactionJob: Job? = null
    private var getTransactionJob: Job? = null

    private val satisfiableMap: MutableMap<String, Boolean> = mutableMapOf()
    private val signedHash: MutableMap<String, Boolean> = mutableMapOf()
    private val keySetStatues: MutableMap<String, KeySetStatus> = mutableMapOf()
    private val coinIdsGroups: MutableMap<String, CoinsGroup> = mutableMapOf()

    private fun getState() = state.value

    init {
        viewModelScope.launch {
            BlockListener.getBlockChainFlow().collect {
                getTransactionInfo()
            }
        }
        viewModelScope.launch {
            TransactionListener.transactionUpdateFlow.collect {
                if (it.txId == txId) {
                    getTransactionInfo()
                }
            }
        }
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                if (event is PushEvent.ServerTransactionEvent) {
                    if (event.transactionId == txId) {
                        Timber.d("ServerTransactionEvent")
                        delay(2000L)
                        getTransactionInfo()
                    }
                } else if (event is PushEvent.TransactionCancelled) {
                    if (event.transactionId == txId) {
                        _event.emit(DeleteTransactionSuccess(true))
                    }
                } else if (event is PushEvent.SignedChanged) {
                    if (getState().signers.any { signer -> signer.fingerPrint == event.xfp }) {
                        loadWallet()
                    }
                }
            }
        }
        viewModelScope.launch {
            state.map { it.transaction.inputs }
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect {
                    getCoinsFromTxInputsUseCase(GetCoinsFromTxInputsUseCase.Params(walletId, it))
                        .onSuccess { coins ->
                            _state.update { state -> state.copy(txInputCoins = coins) }
                        }
                }
        }
        viewModelScope.launch {
            state.filter { it.transaction.txId.isNotEmpty() && it.wallet.id.isNotEmpty() }
                .map { it.transaction.status }
                .distinctUntilChanged()
                .collect {
                    val wallet = getState().wallet
                    if (wallet.miniscript.isNotEmpty()) {
                        getMiniscriptInfo(
                            transaction = getTransaction(),
                            wallet = wallet,
                            isValueKeySetDisable = wallet.isValueKeySetDisable,
                            addressType = wallet.addressType,
                            defaultKeySetIndex = getState().defaultKeySetIndex
                        )
                    }
                }
        }
        viewModelScope.launch {
            getSavedAddressListLocalUseCase(Unit)
                .map { it.getOrThrow() }
                .collect { savedAddresses ->
                    _state.update { it.copy(savedAddress = savedAddresses.associate { saved -> saved.address to saved.label }) }
                }
        }
        viewModelScope.launch {
            while (true) {
                getChainTipUseCase(Unit)
                    .onSuccess { chainTip ->
                        _minscriptState.update { it.copy(chainTip = chainTip) }
                    }.onFailure {
                        Timber.e(it, "Failed to get chain tip")
                    }
                delay(60000) // Refresh every minute
            }
        }
    }

    fun init(
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?,
        isClaimingInheritance: Boolean = false
    ) {
        this.walletId = walletId
        this.txId = txId
        this.initEventId = initEventId
        this.roomId = roomId
        this.initTransaction = transaction
        this.isClaimingInheritance = isClaimingInheritance

        if (isSharedTransaction()) {
            getContacts()
        } else {
            loadWallet()
        }
        loadInitEventIdIfNeed()
        initTransaction?.let { initTransaction ->
            _state.update { it.copy(transaction = initTransaction) }
        }
        loadMasterSigner()
        if (isClaimingInheritance.not()) listenTransactionChanged()
        getAllTags()
        getAllCoins()
        getGroupMembers()
    }

    private fun getGroupMembers() {
        val groupId = assistedWalletManager.getGroupId(walletId) ?: return
        viewModelScope.launch {
            getGroupUseCase(GetGroupUseCase.Params(groupId = groupId))
                .map { it.getOrElse { null } }
                .distinctUntilChanged()
                .collect { group ->
                    val members = group?.members.orEmpty()
                        .filter {
                            it.role != AssistedWalletRole.OBSERVER.name
                                    && isMatchingEmailOrUserName(it.emailOrUsername).not()
                                    && it.isPendingRequest().not()
                        }
                    _state.update {
                        it.copy(
                            members = members,
                            userRole = byzantineGroupUtils.getCurrentUserRole(group).toRole
                        )
                    }
                }
        }
    }

    fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { allTags ->
                _state.update { it.copy(tags = allTags.associateBy { tag -> tag.id }) }
            }
        }
    }

    fun getAllCoins() {
        viewModelScope.launch {
            getAllCoinUseCase(walletId).onSuccess { coins ->
                _state.update { it.copy(coins = coins.filter { coin -> coin.txid == txId }) }
            }
        }
    }

    private fun listenTransactionChanged() {
        if (isAssistedWallet()) {
            viewModelScope.launch {
                state.collect {
                    if (it.transaction.txId.isNotEmpty()) {
                        val signedCount = it.transaction.signers.count { entry -> entry.value }
                        if (initNumberOfSignedKey == INVALID_NUMBER_OF_SIGNED) {
                            initNumberOfSignedKey = signedCount
                        } else if (signedCount > initNumberOfSignedKey) {
                            initNumberOfSignedKey = signedCount
                            if (signedCount > 0 && (it.transaction.status.isPendingSignatures() || it.transaction.status.canBroadCast())) {
                                requestServerSignTransaction(it.transaction.psbt)
                            }
                        }
                        val signedTime = it.serverTransaction?.signedInMilis ?: 0L
                        handleSignTime(signedTime)
                    }
                }
            }
        }
    }

    private fun handleSignTime(signedTime: Long) {
        if (signedTime > 0L && signedTime > System.currentTimeMillis()) {
            reloadTransactionJob?.cancel()
            reloadTransactionJob = viewModelScope.launch {
                val delay = signedTime - System.currentTimeMillis() + 3000L
                delay(delay)
                getTransactionInfo()
            }
        }
    }

    private fun requestServerSignTransaction(psbt: String) {
        viewModelScope.launch {
            signServerTransactionUseCase(
                SignServerTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    txId = txId,
                    psbt = psbt
                )
            ).onSuccess { extendedTransaction ->
                _event.emit(
                    SignTransactionSuccess(
                        status = extendedTransaction.transaction.status,
                        serverSigned = isSignByServerKey(extendedTransaction.transaction)
                    )
                )
                _state.update {
                    it.copy(
                        transaction = extendedTransaction.transaction,
                        serverTransaction = extendedTransaction.serverTransaction
                    )
                }
                if (isMiniscriptWallet()) {
                    getSignedSigners(extendedTransaction.transaction)
                }
            }.onFailure {
                if (it.isNoInternetException) {
                    _event.emit(NoInternetConnection)
                } else {
                    _event.emit(TransactionDetailsError(it.message.orUnknownError()))
                }
            }
        }
    }

    private fun loadMasterSigner() {
        viewModelScope.launch {
            getMasterSignersUseCase.execute().collect {
                masterSigners = it
            }
        }
    }

    private fun loadWallet() {
        if (walletId.isEmpty() || initTransaction != null) return
        viewModelScope.launch {
            getWalletUseCase.execute(walletId).collect { wallet ->
                _minscriptState.update { it.copy(isMiniscriptWallet = wallet.isMiniscriptWallet()) }
                val account = accountManager.getAccount()
                _state.update {
                    it.copy(
                        wallet = wallet.wallet,
                        hideFiatCurrency = assistedWalletManager.getBriefWallet(walletId)?.hideFiatCurrency
                            ?: false,
                    )
                }
                val defaultKeySetIndex =
                    if (wallet.wallet.walletTemplate != WalletTemplate.DISABLE_KEY_PATH && wallet.wallet.addressType.isTaproot()) {
                        loadKeySetSelection()
                    } else 0
                _state.update {
                    it.copy(
                        defaultKeySetIndex = defaultKeySetIndex,
                    )
                }
                if (wallet.wallet.miniscript.isEmpty()) {
                    val signers = wallet.wallet.signers.map { signer ->
                        singleSignerMapper(signer)
                    }
                    if (wallet.roomWallet != null) {
                        _state.update {
                            it.copy(signers = wallet.roomWallet?.joinKeys().orEmpty().map { key ->
                                key.retrieveInfo(
                                    key.chatId == account.chatId, signers, contacts
                                )
                            })
                        }
                    } else {
                        _state.update { it.copy(signers = signers) }
                    }
                }
            }
        }
    }

    private suspend fun getMiniscriptInfo(
        transaction: Transaction,
        wallet: Wallet,
        isValueKeySetDisable: Boolean,
        addressType: AddressType,
        defaultKeySetIndex: Int
    ) {
        satisfiableMap.clear()
        signedHash.clear()
        keySetStatues.clear()
        if (addressType.isTaproot() && !isValueKeySetDisable && defaultKeySetIndex == 0) {
            val signers = wallet.signers.take(wallet.totalRequireSigns).map { signer ->
                singleSignerMapper(signer)
            }
            _state.update {
                it.copy(
                    signers = signers,
                )
            }
            _minscriptState.update {
                it.copy(
                    signerMap = signers.mapIndexed { index, model -> "$MusigKeyPrefix$index" to model }
                        .toMap(),
                )
            }
        } else {
            getTransactionTimeLock(walletId)
            getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
                val signerMap = parseSignersFromScriptNode(result.scriptNode, transaction)
                _state.update {
                    it.copy(
                        signers = signerMap.values.toList(),
                    )
                }
                _minscriptState.update {
                    it.copy(
                        signerMap = signerMap,
                        scriptNode = result.scriptNode,
                        satisfiableMap = satisfiableMap,
                        signedHash = signedHash,
                        keySetStatues = keySetStatues,
                        collapsedNode = getCollapsedNode(result.scriptNode),
                        topLevelDisableNode = getTopLevelDisabledNode(result.scriptNode),
                        coinGroups = coinIdsGroups,
                    )
                }
            }
        }
    }

    private fun getTransactionTimeLock(walletId: String) {
        viewModelScope.launch {
            getTimelockedUntilUseCase(
                GetTimelockedUntilUseCase.Params(
                    walletId = walletId,
                    txId = txId
                )
            ).onSuccess { (lockedTime, lockedBase) ->
                if (lockedBase != MiniscriptTimelockBased.NONE) {
                    walletLockedBase.put(walletId, lockedBase)
                }
                timelockTransactionCache.put(txId, lockedTime)
                _minscriptState.update {
                    it.copy(
                        lockedTime = lockedTime,
                        lockedBase = lockedBase
                    )
                }
            }.onFailure {
                Timber.e(it, "Failed to get timelocked until")
            }
        }
        viewModelScope.launch {
            getChainTipUseCase(Unit)
                .onSuccess { chainTip ->
                    _minscriptState.update { it.copy(chainTip = chainTip) }
                }.onFailure {
                    Timber.e(it, "Failed to get chain tip")
                }
        }
    }

    private fun getCollapsedNode(
        scriptNode: ScriptNode,
    ): ScriptNode? {
        val targetTypes = setOf(
            ScriptNodeType.ANDOR.name,
            ScriptNodeType.OR.name,
            ScriptNodeType.OR_TAPROOT.name
        )
        val queue: ArrayDeque<ScriptNode> = ArrayDeque()
        queue.add(scriptNode)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.type in targetTypes) {
                // TODO GetCoinsGroupedBySubPoliciesUseCase
                return current.subs.drop(
                    if (current.type == ScriptNodeType.ANDOR.name) 1 else 0
                ).find { satisfiableMap[it.idString] == false }
            }
            current.subs.forEach { queue.add(it) }
        }
        return null
    }

    private fun getTopLevelDisabledNode(
        scriptNode: ScriptNode,
    ): ScriptNode? {
        val queue: ArrayDeque<ScriptNode> = ArrayDeque()
        queue.add(scriptNode)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (satisfiableMap[current.idString] == false) return current
            current.subs.forEach { queue.add(it) }
        }
        return null
    }

    private suspend fun parseSignersFromScriptNode(
        node: ScriptNode,
        transaction: Transaction
    ): Map<String, SignerModel> {
        if (!satisfiableMap.containsKey(node.idString)) {
            if (transaction.raw.isNotEmpty()) {
                satisfiableMap[node.idString] = true
            } else {
                satisfiableMap[node.idString] = isScriptNodeSatisfiableUseCase(
                    IsScriptNodeSatisfiableUseCase.Params(
                        nodeId = node.id.toIntArray(),
                        walletId = walletId,
                        psbt = transaction.psbt
                    )
                ).getOrDefault(false)
            }
        }

        if (node.isPreImageNode) {
            val isRevealed = isPreimageRevealedUseCase(
                IsPreimageRevealedUseCase.Params(
                    walletId = walletId,
                    txId = txId,
                    hash = node.data
                )
            ).getOrDefault(false)
            signedHash[node.idString] = isRevealed
        }

        if (node.type == ScriptNodeType.MUSIG.name) {
            getKeySetStatusUseCase(
                GetKeySetStatusUseCase.Params(
                    walletId = walletId,
                    nodeId = node.id.toIntArray(),
                    txId = txId
                )
            ).onSuccess { status ->
                keySetStatues[node.idString] = status
            }
        }

        if (satisfiableMap[node.idString] == false) {
            node.subs.forEach { subNode ->
                satisfiableMap[subNode.idString] = false
            }
        }

        // special case for ANDOR node
        if (node.type == ScriptNodeType.ANDOR.name && node.subs.size == 3) {
            val isSatisfiable = isScriptNodeSatisfiableUseCase(
                IsScriptNodeSatisfiableUseCase.Params(
                    nodeId = node.subs[0].id.toIntArray(),
                    walletId = walletId,
                    psbt = transaction.psbt
                )
            ).getOrDefault(false)
            if (isSatisfiable) {
                satisfiableMap[node.subs[0].idString] = true
                satisfiableMap[node.subs[1].idString] = true
                satisfiableMap[node.subs[2].idString] = true
            } else {
                satisfiableMap[node.subs[0].idString] = false
                satisfiableMap[node.subs[1].idString] = false
                satisfiableMap[node.subs[2].idString] = true
            }
        }
        val signerMap = mutableMapOf<String, SignerModel>()
        node.keys.forEach { key ->
            parseSignerStringUseCase(key).getOrNull()?.let { signer ->
                signerMap[key] = singleSignerMapper(signer)
            }
        }
        node.subs.forEach { subNode ->
            signerMap.putAll(parseSignersFromScriptNode(subNode, transaction))
        }
        return signerMap
    }

    private suspend fun loadKeySetSelection(): Int {
        return getTaprootKeySetSelectionUseCase(txId).getOrElse { null } ?: 0
    }

    fun setCurrentSigner(signer: SignerModel) {
        savedStateHandle[KEY_CURRENT_SIGNER] = signer
    }

    private fun loadInitEventIdIfNeed() {
        if (initEventId.isEmpty() && roomId.isNotEmpty()) {
            viewModelScope.launch {
                val result =
                    getPendingTransactionUseCase(GetPendingTransactionUseCase.Data(roomId, txId))
                if (result.isSuccess) {
                    initEventId = result.getOrThrow().initEventId
                }
            }
        }
    }

    fun getInitEventId(): String {
        return initEventId
    }

    private fun getContacts() {
        viewModelScope.launch {
            getContactsUseCase.execute().catch { contacts = emptyList() }.collect {
                contacts = it
                loadWallet()
            }
        }
    }

    fun getTransactionInfo() {
        if (isClaimingInheritance) return
        if (initTransaction != null) {
            getTransactionFromNetwork()
        } else {
            loadLocalTransaction()
        }
    }

    fun getTransaction() = getState().transaction

    fun updateTransactionMemo(newMemo: String) {
        viewModelScope.launch {
            _event.emit(LoadingEvent)
            val result = updateTransactionMemo(
                UpdateTransactionMemo.Data(
                    assistedWalletManager.getGroupId(walletId),
                    walletId,
                    isAssistedWallet(),
                    txId,
                    newMemo
                )
            )
            if (result.isSuccess) {
                updateTransaction(getState().transaction.copy(memo = newMemo))
                _event.emit(UpdateTransactionMemoSuccess(newMemo))
            } else {
                _event.emit(UpdateTransactionMemoFailed(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun getTransactionFromNetwork() {
        if (getTransactionJob?.isActive == true) return
        getTransactionJob = viewModelScope.launch {
            val result = getTransactionFromNetworkUseCase(txId)
            if (result.isSuccess) {
                _state.update { it.copy(transaction = result.getOrThrow()) }
            }
        }
    }

    private fun loadLocalTransaction() {
        if (getTransactionJob?.isActive == true) return
        getTransactionJob = viewModelScope.launch {
            getTransactionUseCase.execute(
                groupId = assistedWalletManager.getGroupId(walletId),
                walletId = walletId,
                txId = txId,
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            ).flowOn(IO).onException {
                if (it is NCNativeException && it.message.contains("-2003")) {
                    _event.emit(DeleteTransactionSuccess())
                } else if ((it as? NunchukApiException)?.code == ApiErrorCode.TRANSACTION_CANCEL) {
                    handleDeleteTransactionEvent(isCancel = true, onlyLocal = true)
                } else if (it.isNoInternetException.not()) {
                    _event.emit(TransactionDetailsError(it.message.orEmpty()))
                }
            }.collect {
                updateTransaction(
                    transaction = it.transaction,
                    serverTransaction = it.serverTransaction,
                )
                getSignedSigners(it.transaction)
            }
        }
    }

    /**
     * For miniscript wallet, get the signers who have signed the transaction
     */
    private fun getSignedSigners(tx: Transaction) {
        viewModelScope.launch {
            val pendingNodeId = savedStateHandle.get<String>(KEY_NODE_ID).orEmpty()
            if (miniscriptState.value.keySetStatues[pendingNodeId] != null) {
                getKeySetStatusUseCase(
                    GetKeySetStatusUseCase.Params(
                        walletId = walletId,
                        nodeId = pendingNodeId.split(".").mapNotNull { it.toIntOrNull() }
                            .toIntArray(),
                        txId = txId
                    )
                ).onSuccess { status ->
                    val updatedMap = miniscriptState.value.keySetStatues.toMutableMap()
                    updatedMap[pendingNodeId] = status
                    _minscriptState.update { it.copy(keySetStatues = updatedMap) }
                }.onFailure {
                    Timber.e(
                        it, "Failed to get key set status"
                    )
                }
                savedStateHandle.remove<String>(KEY_NODE_ID)
            }
            if (tx.raw.isNotEmpty()) {
                getTransactionSignersUseCase(
                    GetTransactionSignersUseCase.Params(
                        walletId = walletId,
                        txId = txId
                    )
                ).onSuccess { signers ->
                    val signedSignersMap = signers.associate { signer ->
                        signer.masterFingerprint to true
                    }
                    _state.update {
                        it.copy(
                            transaction = tx.copy(signedSigner = signers)
                        )
                    }
                    _minscriptState.update { it.copy(signedSigners = signedSignersMap) }
                }.onFailure {
                    Timber.e(it, "Failed to get transaction signers")
                }
            }
        }
    }

    fun updateServerTransaction(serverTransaction: ServerTransaction?) {
        _state.update { it.copy(serverTransaction = serverTransaction) }
    }

    private fun updateTransaction(
        transaction: Transaction,
        serverTransaction: ServerTransaction? = getState().serverTransaction,
    ) {
        _state.update {
            it.copy(
                transaction = transaction,
                serverTransaction = serverTransaction,
            )
        }
    }

    fun handleBroadcastEvent() {
        if (isSharedTransaction()) {
            broadcastSharedTransaction()
        } else {
            broadcastPersonalTransaction()
        }
    }

    private fun broadcastPersonalTransaction() {
        viewModelScope.launch {
            _event.emit(LoadingEvent)
            broadcastTransactionUseCase.execute(walletId, txId).flowOn(IO)
                .onException { _event.emit(TransactionDetailsError(it.message.orEmpty())) }
                .collect {
                    updateTransaction(it)

                    val info = if (it.status != TransactionStatus.NETWORK_REJECTED) {
                        runCatching { reviewManager.requestReview() }.getOrNull()
                    } else {
                        null
                    }
                    _event.emit(BroadcastTransactionSuccess(reviewInfo = info))
                }
        }
    }

    private fun broadcastSharedTransaction() {
        viewModelScope.launch {
            _event.emit(LoadingEvent)
            broadcastRoomTransactionUseCase.execute(initEventId).flowOn(IO)
                .onException { _event.emit(TransactionDetailsError(it.message.orEmpty())) }
                .collect { _event.emit(BroadcastTransactionSuccess(roomId)) }
        }
    }

    fun handleViewBlockchainEvent() = viewModelScope.launch {
        val result = getBlockchainExplorerUrlUseCase(txId)
        if (result.isSuccess) {
            _event.emit(ViewBlockchainExplorer(result.getOrThrow()))
        } else {
            _event.emit(TransactionDetailsError(result.exceptionOrNull()?.message.orEmpty()))
        }
    }

    fun handleMenuMoreEvent() = viewModelScope.launch {
        val status = getState().transaction.status
        _event.emit(
            PromptTransactionOptions(
                isPendingTransaction = status.isPending(),
                isPendingConfirm = status.isPendingConfirm(),
                isRejected = status.isRejected(),
                canBroadcast = status.canBroadCast(),
                txStatus = status.name,
            )
        )
    }

    fun handleDeleteTransactionEvent(isCancel: Boolean = true, onlyLocal: Boolean = false) {
        viewModelScope.launch {
            _event.emit(LoadingEvent)
            val result = deleteTransactionUseCase(
                DeleteTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    txId = txId,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId) && !onlyLocal
                )
            )
            if (result.isSuccess) {
                _event.emit(DeleteTransactionSuccess(isCancel))
            } else {
                _event.emit(TransactionDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun cancelScheduleBroadcast() {
        viewModelScope.launch {
            val result = cancelScheduleBroadcastTransactionUseCase(
                CancelScheduleBroadcastTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    transactionId = txId,
                )
            )
            if (result.isSuccess) {
                _state.update { it.copy(serverTransaction = result.getOrThrow()) }
                _event.emit(CancelScheduleBroadcastTransactionSuccess)
            } else {
                _event.emit(TransactionDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleSignSoftwareKey(signer: SignerModel) {
        viewModelScope.launch {
            val fingerPrint = signer.fingerPrint
            val device =
                masterSigners.firstOrNull { it.device.masterFingerprint == fingerPrint }?.device
                    ?: return@launch
            if (device.needPassPhraseSent) {
                _event.emit(PromptInputPassphrase {
                    viewModelScope.launch {
                        sendSignerPassphrase.execute(signer.id, it).flowOn(IO)
                            .onException { _event.emit(TransactionDetailsError(it.message.orEmpty())) }
                            .collect { signTransaction(device, signer.id) }
                    }
                })
            } else {
                signTransaction(device, signer.id)
            }
        }
    }

    private fun signTransaction(device: Device, signerId: String) {
        if (isSharedTransaction()) {
            signRoomTransaction(device, signerId)
        } else {
            signPersonalTransaction(device, signerId)
        }
    }

    private fun isSharedTransaction() = roomId.isNotEmpty()

    fun exportTransactionToFile(isSaveFile: Boolean) {
        viewModelScope.launch {
            _event.emit(LoadingEvent)
            when (val result = createShareFileUseCase.execute("${walletId}_${txId}.psbt")) {
                is Success -> exportTransaction(result.data, isSaveFile)
                is Error -> {
                    val message =
                        result.exception.messageOrUnknownError()
                    _event.emit(TransactionError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
            }
        }
    }

    private fun exportTransaction(filePath: String, isSaveFile: Boolean) {
        viewModelScope.launch {
            when (val result = exportTransactionUseCase.execute(walletId, txId, filePath)) {
                is Success -> {
                    if (isSaveFile) {
                        saveLocalFile(filePath)
                    } else {
                        _event.emit(ExportToFileSuccess(filePath))
                    }
                }

                is Error -> {
                    val message = result.exception.messageOrUnknownError()
                    _event.emit(TransactionError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
            }
        }
    }

    private fun saveLocalFile(filePath: String) {
        viewModelScope.launch {
            val result = saveLocalFileUseCase(
                SaveLocalFileUseCase.Params(
                    fileName = "${walletId}_${txId}.psbt",
                    filePath = filePath
                )
            )
            _event.emit(TransactionDetailsEvent.SaveLocalFile(result.isSuccess))
        }
    }

    private fun signRoomTransaction(device: Device, signerId: String) {
        viewModelScope.launch {
            signRoomTransactionUseCase.execute(initEventId = initEventId, device = device, signerId)
                .flowOn(IO).onException {
                    fireSignError(it)
                }.collect {
                    _event.emit(SignTransactionSuccess(roomId))
                }
        }
    }

    private fun signPersonalTransaction(device: Device, signerId: String) {
        viewModelScope.launch {
            val isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            val result = signTransactionUseCase(
                SignTransactionUseCase.Param(
                    walletId = walletId,
                    txId = txId,
                    device = device,
                    signerId = signerId,
                    isAssistedWallet = isAssistedWallet
                )
            )
            if (result.isSuccess) {
                updateTransaction(
                    transaction = result.getOrThrow(),
                )
                if (isMiniscriptWallet()) {
                    getSignedSigners(result.getOrThrow())
                }
                _event.emit(SignTransactionSuccess())
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    fun handleSignByTapSigner(isoDep: IsoDep?, inputCvc: String) {
        isoDep ?: return
        if (isSharedTransaction()) {
            signRoomTransactionTapSigner(isoDep, inputCvc)
        } else {
            signPersonTapSignerTransaction(isoDep, inputCvc)
        }
    }

    fun handleExportTransactionToMk4(ndef: Ndef) {
        viewModelScope.launch {
            _event.emit(NfcLoadingEvent(true))
            val result = exportPsbtToMk4UseCase(ExportPsbtToMk4UseCase.Data(walletId, txId, ndef))
            if (result.isSuccess) {
                _event.emit(ExportTransactionToMk4Success)
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    fun handleImportTransactionFromMk4(records: List<NdefRecord>) {
        val signer = savedStateHandle.get<SignerModel>(KEY_CURRENT_SIGNER) ?: return
        viewModelScope.launch {
            _event.emit(LoadingEvent)
            val result = importTransactionFromMk4UseCase(
                ImportTransactionFromMk4UseCase.Data(
                    walletId, records, initEventId, signer.fingerPrint
                )
            )
            val transaction = result.getOrNull()
            if (result.isSuccess && transaction != null) {
                _state.update { it.copy(transaction = transaction) }
                if (isMiniscriptWallet()) {
                    getSignedSigners(transaction)
                }
                _event.emit(ImportTransactionFromMk4Success)
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    private fun signRoomTransactionTapSigner(isoDep: IsoDep, inputCvc: String) {
        viewModelScope.launch {
            _event.emit(NfcLoadingEvent())
            val result = signRoomTransactionByTapSignerUseCase(
                SignRoomTransactionByTapSignerUseCase.Data(
                    isoDep, inputCvc, initEventId
                )
            )
            if (result.isSuccess) {
                _event.emit(SignTransactionSuccess(roomId))
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    private fun signPersonTapSignerTransaction(isoDep: IsoDep, inputCvc: String) {
        viewModelScope.launch {
            _event.emit(NfcLoadingEvent())
            val isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            val result = signTransactionByTapSignerUseCase(
                SignTransactionByTapSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = inputCvc,
                    walletId = walletId,
                    txId = txId,
                    isAssistedWallet = isAssistedWallet
                )
            )
            if (result.isSuccess) {
                updateTransaction(
                    transaction = result.getOrThrow(),
                )
                if (isMiniscriptWallet()) {
                    getSignedSigners(result.getOrThrow())
                }
                _event.emit(SignTransactionSuccess())
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    fun currentSigner() = savedStateHandle.get<SignerModel>(KEY_CURRENT_SIGNER)

    private suspend fun fireSignError(e: Throwable?) {
        val message = e?.message.orEmpty()
        _event.emit(TransactionDetailsError(message, e))
        CrashlyticsReporter.recordException(TransactionException(message))
    }

    fun isAssistedWallet() = assistedWalletManager.isActiveAssistedWallet(walletId)
    private fun isIronHandWallet() =
        getState().signers.size == 3 && getState().signers.any { it.type == SignerType.SERVER }

    fun isSupportScheduleBroadcast() = isAssistedWallet() && !isIronHandWallet()

    fun isScheduleBroadcast() = (getState().serverTransaction?.broadcastTimeInMilis ?: 0L) > 0L

    fun getMembers() = getState().members

    fun importTransactionViaFile(walletId: String, uri: Uri) {
        viewModelScope.launch {
            val file = withContext(dispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)
            } ?: return@launch
            importTransactionUseCase(
                ImportTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    filePath = file.absolutePath,
                    isAssistedWallet = isAssistedWallet()
                )
            ).onSuccess {
                getTransactionInfo()
                if (isMiniscriptWallet()) {
                    getSignedSigners(getState().transaction)
                }
                _event.emit(ImportTransactionSuccess)
            }.onFailure {
                _event.emit(TransactionError(it.readableMessage()))
            }
        }
    }

    fun getRawTransaction() = viewModelScope.launch {
        val result = getRawTransactionUseCase(GetRawTransactionUseCase.Param(walletId, txId))
        if (result.isSuccess) {
            _event.emit(GetRawTransactionSuccess(result.getOrThrow()))
        } else {
            _event.emit(
                TransactionError(
                    result.exceptionOrNull()?.readableMessage().orUnknownError()
                )
            )
        }
    }

    fun requestSignatureTransaction(membershipId: String) = viewModelScope.launch {
        _event.emit(LoadingEvent)
        requestSignatureTransactionUseCase(
            RequestSignatureTransactionUseCase.Param(
                groupId = assistedWalletManager.getGroupId(walletId).orEmpty(),
                walletId = walletId,
                transactionId = txId,
                membershipId = membershipId
            )
        ).onSuccess {
            _event.emit(TransactionDetailsEvent.RequestSignatureTransactionSuccess)
        }.onFailure {
            _event.emit(TransactionError(it.readableMessage().orUnknownError()))
        }
    }

    fun allTags() = getState().tags
    fun coins() = getState().coins

    fun getUserRole() = getState().userRole

    private fun isSignByServerKey(transaction: Transaction): Boolean {
        val fingerPrint =
            getState().signers.find { it.type == SignerType.SERVER }?.fingerPrint.orEmpty()
        return transaction.signers[fingerPrint] == true
    }

    private fun isMatchingEmailOrUserName(emailOrUsername: String) =
        emailOrUsername == accountManager.getAccount().email
                || emailOrUsername == accountManager.getAccount().username

    fun getWalletPlan() = assistedWalletManager.getWalletPlan(walletId)

    fun handleSignPortalKey(psbt: String) {
        viewModelScope.launch {
            importPsbtUseCase(
                ImportPsbtUseCase.Param(
                    psbt = psbt,
                    walletId = walletId,
                )
            ).onSuccess {
                val signer = currentSigner() ?: return@onSuccess
                if (it.status != TransactionStatus.READY_TO_BROADCAST && it.signers[signer.fingerPrint] == false) {
                    _event.emit(TransactionError("Wallet has not registered to Portal yet"))
                } else {
                    updateTransaction(transaction = it)
                    if (isMiniscriptWallet()) {
                        getSignedSigners(it)
                    }
                    _event.emit(SignTransactionSuccess())
                }
            }.onFailure {
                _event.emit(TransactionError(it.readableMessage()))
            }
        }
    }

    fun handlePreimageSuccess(scriptNodeId: String) {
        _minscriptState.update {
            it.copy(
                signedHash = signedHash.toMutableMap().apply {
                    put(scriptNodeId, true)
                },
            )
        }
        loadLocalTransaction()
    }

    fun showReview(activity: Activity, reviewInfo: ReviewInfo, doneCallback: () -> Unit) {
        val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
        flow.addOnCompleteListener {
            doneCallback()
        }
    }

    fun isTimelockedActive(): Boolean {
        return _minscriptState.value.isTimelockedActive
    }

    fun isMiniscriptWallet(): Boolean {
        return _minscriptState.value.isMiniscriptWallet
    }

    fun setPendingNodeId(nodeId: String) {
        savedStateHandle[KEY_NODE_ID] = nodeId
    }

    companion object {
        private const val INVALID_NUMBER_OF_SIGNED = -1
        private const val KEY_CURRENT_SIGNER = "current_signer"
        private const val KEY_NODE_ID = "node_id"
    }
}