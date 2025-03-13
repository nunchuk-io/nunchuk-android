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

package com.nunchuk.android.wallet.components.config

import android.content.Context
import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesDeleteAssistedWalletUseCase
import com.nunchuk.android.core.domain.membership.DeleteAssistedWalletUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.domain.wallet.GetWalletBsmsUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.wallet.InvoiceInfo
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.joinKeys
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.usecase.GetTransactionHistoryUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetDeprecatedGroupWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.SetBackUpBannerWalletIdsUseCase
import com.nunchuk.android.usecase.membership.ExportCoinControlBIP329UseCase
import com.nunchuk.android.usecase.membership.ExportTxCoinControlUseCase
import com.nunchuk.android.usecase.membership.ForceRefreshWalletUseCase
import com.nunchuk.android.usecase.membership.ImportCoinControlBIP329UseCase
import com.nunchuk.android.usecase.membership.ImportTxCoinControlUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.ExportInvoices
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.retrieveInfo
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class WalletConfigViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val accountManager: AccountManager,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val forceRefreshWalletUseCase: ForceRefreshWalletUseCase,
    private val calculateRequiredSignaturesDeleteAssistedWalletUseCase: CalculateRequiredSignaturesDeleteAssistedWalletUseCase,
    private val deleteAssistedWalletUseCase: DeleteAssistedWalletUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val importTxCoinControlUseCase: ImportTxCoinControlUseCase,
    private val exportTxCoinControlUseCase: ExportTxCoinControlUseCase,
    private val importCoinControlBIP329UseCase: ImportCoinControlBIP329UseCase,
    private val exportCoinControlBIP329UseCase: ExportCoinControlBIP329UseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    getContactsUseCase: GetContactsUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getGroupWalletsUseCase: GetGroupWalletsUseCase,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    private val getWalletBsmsUseCase: GetWalletBsmsUseCase,
    private val getDeprecatedGroupWalletsUseCase: GetDeprecatedGroupWalletsUseCase,
    private val setBackUpBannerWalletIdsUseCase: SetBackUpBannerWalletIdsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletConfigState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<WalletConfigEvent>()
    val event = _event.asSharedFlow()

    private val walletId: String
        get() = savedStateHandle.get<String>("EXTRA_WALLET_ID").orEmpty()

    private val contacts = getContactsUseCase.execute()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _progressFlow = MutableStateFlow(0 to 0)
    val progressFlow: StateFlow<Pair<Int, Int>> get() = _progressFlow

    private var exportInvoicesJob: Job? = null

    private val exportInvoices = ExportInvoices(context)

    private val accountInfo by lazy { accountManager.getAccount() }

    private fun getState() = _state.value

    init {
        getWalletDetails()
        getUserRole()
        getWalletAlias()

        viewModelScope.launch {
            exportInvoices.progressFlow.collect { progress ->
                _progressFlow.emit(progress)
            }
        }
        viewModelScope.launch {
            getGroupWalletsUseCase(Unit).onSuccess { wallets ->
                _state.update {
                    it.copy(isGroupSandboxWallet = wallets.any { wallet -> wallet.id == walletId })
                }
            }
        }
        viewModelScope.launch {
            getDeprecatedGroupWalletsUseCase(Unit).onSuccess { deprecatedWallets ->
                _state.update { it.copy(isDeprecatedGroupWallet = deprecatedWallets.any { it == walletId }) }
            }
        }
    }

    private fun getWalletAlias() {
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).mapNotNull { wallets ->
                wallets.getOrElse { emptyList() }.find { it.localId == walletId }
            }.collect { wallet ->
                _state.update { it.copy(alias = wallet.alias) }
                getWalletDetails()
            }
        }
    }

    private var getWalletDetailJob: Job? = null
    private fun getWalletDetails() {
        if (getWalletDetailJob?.isActive == true) return
        getWalletDetailJob = viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .onEach {
                    if (it.roomWallet != null) {
                        loadContact()
                    }
                }
                .map {
                    WalletConfigState(
                        walletExtended = it,
                        signers = mapSigners(it.wallet.signers, it.roomWallet),
                        isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId),
                        isInactiveAssistedWallet = assistedWalletManager.isInactiveAssistedWallet(
                            walletId
                        ),
                        assistedWallet = assistedWalletManager.getBriefWallet(walletId)
                    )
                }
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { state ->
                    getTransactionHistory()
                    _state.update {
                        it.copy(
                            walletExtended = state.walletExtended,
                            signers = state.signers,
                            isAssistedWallet = state.isAssistedWallet,
                            isInactiveAssistedWallet = state.isInactiveAssistedWallet,
                            assistedWallet = state.assistedWallet
                        )
                    }
                }
        }
    }

    private fun getUserRole() {
        viewModelScope.launch {
            val groupId = getGroupId() ?: return@launch
            getGroupUseCase(GetGroupUseCase.Params(groupId = groupId))
                .map { it.getOrElse { null } }
                .distinctUntilChanged()
                .collect { group ->
                    val role = byzantineGroupUtils.getCurrentUserRole(group)
                    _state.update { it.copy(role = role, group = group) }
                }
        }
    }

    private fun loadContact() {
        viewModelScope.launch {
            contacts.collect {
                mapSigners(
                    getState().walletExtended.wallet.signers,
                    getState().walletExtended.roomWallet
                )
            }
        }
    }

    fun verifyPassword(password: String, signer: SignerModel) {
        viewModelScope.launch {
            _event.emit(WalletConfigEvent.Loading(true))
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    TargetAction.UPDATE_SERVER_KEY.name,
                    password
                )
            )
            _event.emit(WalletConfigEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(
                    WalletConfigEvent.VerifyPasswordSuccess(
                        result.getOrThrow().orEmpty(),
                        signer,
                        assistedWalletManager.getGroupId(walletId)
                    )
                )
            } else {
                _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun verifyPasswordToReplaceKey(password: String) {
        viewModelScope.launch {
            _event.emit(WalletConfigEvent.Loading(true))
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    TargetAction.REPLACE_KEYS.name,
                    password
                )
            )
            _event.emit(WalletConfigEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(WalletConfigEvent.OpenReplaceKey)
            } else {
                _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun verifyPasswordToDeleteAssistedWallet(password: String) = viewModelScope.launch {
        _event.emit(WalletConfigEvent.Loading(true))
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                TargetAction.DELETE_WALLET.name,
                password
            )
        )
        _event.emit(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            val resultCalculate = calculateRequiredSignaturesDeleteAssistedWalletUseCase(
                CalculateRequiredSignaturesDeleteAssistedWalletUseCase.Param(
                    walletId = walletId,
                    groupId = assistedWalletManager.getGroupId(walletId)
                )
            )
            if (resultCalculate.isSuccess) {
                _state.update { it.copy(verifyToken = result.getOrNull()) }
                _event.emit(
                    WalletConfigEvent.CalculateRequiredSignaturesSuccess(
                        walletId = walletId,
                        requiredSignatures = resultCalculate.getOrThrow().requiredSignatures,
                        type = resultCalculate.getOrThrow().type
                    )
                )
            } else {
                _event.emit(WalletConfigEvent.WalletDetailsError(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
            }
        } else {
            _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun handleEditCompleteEvent(walletName: String) {
        val newWallet = getState().walletExtended.wallet.copy(name = walletName)
        updateWallet(newWallet, UpdateAction.NAME)
    }

    fun updateGapLimit(gapLimit: Int) {
        val newWallet = getState().walletExtended.wallet.copy(gapLimit = gapLimit)
        updateWallet(newWallet, UpdateAction.GAP_LIMIT)
    }

    private fun updateWallet(newWallet: Wallet, updateAction: UpdateAction) =
        viewModelScope.launch {
            updateWalletUseCase(
                UpdateWalletUseCase.Params(
                    wallet = newWallet,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId),
                    groupId = assistedWalletManager.getGroupId(walletId)
                )
            ).onSuccess {
                if (updateAction == UpdateAction.NAME) {
                    _state.update { it.copy(walletExtended = it.walletExtended.copy(wallet = newWallet)) }
                    _event.emit(UpdateNameSuccessEvent)
                } else if (updateAction == UpdateAction.GAP_LIMIT) {
                    _event.emit(WalletConfigEvent.UpdateGapLimitSuccessEvent)
                }
            }.onFailure {
                _event.emit(UpdateNameErrorEvent(it.message.orUnknownError()))
            }
        }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            _event.emit(WalletConfigEvent.Loading(true))
            getTransactionHistoryUseCase.execute(walletId).flowOn(Dispatchers.IO)
                .collect { transactions ->
                    _event.emit(WalletConfigEvent.Loading(false))
                    val isPendingTransactionExisting =
                        transactions.any { it.status.isPending() }
                    _state.update {
                        it.copy(
                            isShowDeleteAssistedWallet = isPendingTransactionExisting.not(),
                            transactions = transactions
                        )
                    }
                }
        }
    }

    private suspend fun showError(t: Throwable) {
        _event.emit(WalletConfigEvent.WalletDetailsError(t.message.orUnknownError()))
    }

    private suspend fun leaveRoom(onDone: suspend () -> Unit) {
        val roomId = getState().walletExtended.roomWallet?.roomId
        if (roomId == null) {
            onDone()
            return
        }
        leaveRoomUseCase.execute(roomId)
            .flowOn(Dispatchers.IO)
            .onException { e -> showError(e) }
            .collect {
                onDone()
            }
    }

    fun handleDeleteWallet() {
        viewModelScope.launch {
            leaveRoom {
                when (val event = deleteWalletUseCase.execute(walletId)) {
                    is Result.Success -> {
                        setBackUpBannerWalletIdsUseCase(walletId)
                        if (isAssistedWallet()) {
                            _event.emit(WalletConfigEvent.DeleteAssistedWalletSuccess)
                        } else {
                            _event.emit(WalletConfigEvent.DeleteWalletSuccess)
                        }
                    }

                    is Result.Error -> showError(event.exception)
                }
            }
        }
    }

    fun isSharedWallet() = getState().walletExtended.isShared

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>,
        roomWallet: RoomWallet? = null,
    ): List<SignerModel> {
        val account = accountManager.getAccount()
        val signers =
            singleSigners.map { it.toModel(isPrimaryKey = isPrimaryKey(it.masterSignerId)) }
                .map { signer ->
                    if (signer.type == SignerType.NFC) signer.copy(
                        cardId = getTapSignerStatusByIdUseCase(
                            signer.id
                        ).getOrNull()?.ident.orEmpty()
                    )
                    else signer
                }

        return roomWallet?.joinKeys()?.map { key ->
            key.retrieveInfo(
                key.chatId == account.chatId, signers, contacts.value
            )
        } ?: signers
    }

    fun forceRefreshWallet() = viewModelScope.launch {
        _event.emit(WalletConfigEvent.Loading(true))
        val result = forceRefreshWalletUseCase(walletId)
        delay(3000L)
        _event.emit(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(WalletConfigEvent.ForceRefreshWalletSuccess)
        } else {
            _event.emit(WalletConfigEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun handleExportBSMS() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute("${walletId}.bsms")) {
                is Result.Success -> exportWalletToFile(walletId, event.data, ExportFormat.BSMS)
                is Result.Error -> showError(event.exception)
            }
        }
    }

    private fun exportWalletToFile(walletId: String, filePath: String, format: ExportFormat) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, format)) {
                is Result.Success -> _event.emit(
                    WalletConfigEvent.UploadWalletConfigEvent(
                        filePath
                    )
                )

                is Result.Error -> showError(event.exception)
            }
        }
    }

    fun saveBSMSToLocal() {
        viewModelScope.launch {
            getWalletBsmsUseCase(_state.value.walletExtended.wallet).onSuccess {
                val result =
                    saveLocalFileUseCase(SaveLocalFileUseCase.Params("${walletId}.bsms", it))
                _event.emit(WalletConfigEvent.SaveLocalFile(result.isSuccess))
            }
        }
    }

    fun deleteAssistedWallet(
        signatures: HashMap<String, String>,
        securityQuestionToken: String,
    ) = viewModelScope.launch {
        val state = getState()
        if (state.verifyToken == null) return@launch
        _event.emit(WalletConfigEvent.Loading(true))
        val result = deleteAssistedWalletUseCase(
            DeleteAssistedWalletUseCase.Param(
                signatures = signatures,
                verifyToken = state.verifyToken,
                securityQuestionToken = securityQuestionToken,
                walletId = walletId,
                groupId = assistedWalletManager.getGroupId(walletId)
            )
        )
        _event.emit(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            handleDeleteWallet()
        } else {
            _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun importCoinControlNunchuk(filePath: String) = viewModelScope.launch {
        _event.emit(WalletConfigEvent.Loading(true))
        val result = importTxCoinControlUseCase(
            ImportTxCoinControlUseCase.Param(
                walletId = walletId,
                data = filePath,
                force = true
            )
        )
        _event.emit(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(WalletConfigEvent.ImportTxCoinControlSuccess)
        } else {
            _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun exportCoinControlNunchuk() = viewModelScope.launch {
        when (val event = createShareFileUseCase.execute("${walletName()}_labels.json")) {
            is Result.Success -> {
                val result = exportTxCoinControlUseCase(
                    ExportTxCoinControlUseCase.Param(
                        walletId = walletId,
                        filePath = event.data
                    )
                )
                if (result.isSuccess) {
                    _event.emit(WalletConfigEvent.ExportTxCoinControlSuccess(event.data))
                } else {
                    _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }

            is Result.Error -> showError(event.exception)
        }
    }

    fun importCoinControlBIP329(filePath: String) = viewModelScope.launch {
        _event.emit(WalletConfigEvent.Loading(true))
        val result = importCoinControlBIP329UseCase(
            ImportCoinControlBIP329UseCase.Param(
                walletId = walletId,
                data = filePath
            )
        )
        _event.emit(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(WalletConfigEvent.ImportTxCoinControlSuccess)
        } else {
            _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun exportCoinControlBIP329() = viewModelScope.launch {
        when (val event = createShareFileUseCase.execute("${walletName()}_BIP329labels.json")) {
            is Result.Success -> {
                val result = exportCoinControlBIP329UseCase(
                    ExportCoinControlBIP329UseCase.Param(
                        walletId = walletId,
                        filePath = event.data
                    )
                )
                if (result.isSuccess) {
                    _event.emit(WalletConfigEvent.ExportTxCoinControlSuccess(event.data))
                } else {
                    _event.emit(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }

            is Result.Error -> showError(event.exception)
        }
    }

    fun exportInvoice(invoiceInfos: List<InvoiceInfo>, fileName: String) {
        exportInvoicesJob?.cancel()
        exportInvoicesJob = viewModelScope.launch(Dispatchers.IO) {
            _progressFlow.emit(0 to invoiceInfos.size)
            when (val event = createShareFileUseCase.execute("$fileName.pdf")) {
                is Result.Success -> {
                    exportInvoices.generatePDF(invoiceInfos, event.data, exportInvoicesJob!!)
                    withContext(Dispatchers.Main) {
                        _event.emit(WalletConfigEvent.ExportInvoiceSuccess(event.data))
                    }
                }

                is Result.Error -> {
                    _event.emit(WalletConfigEvent.WalletDetailsError(event.exception.message.orUnknownError()))
                }
            }
        }
    }

    private fun isPrimaryKey(id: String) =
        accountInfo.loginType == SignInMode.PRIMARY_KEY.value && accountInfo.primaryKeyInfo?.xfp == id

    fun isAssistedWallet() = assistedWalletManager.isActiveAssistedWallet(walletId)

    fun isServerWallet() = assistedWalletManager.getBriefWallet(walletId) != null

    fun isReplacedOrLocked() = assistedWalletManager.getBriefWallet(walletId)
        ?.let { it.status == WalletStatus.LOCKED.name || it.status == WalletStatus.REPLACED.name } == true

    fun isShowDeleteWallet() = getState().isShowDeleteAssistedWallet || isAssistedWallet().not()

    fun getGroupId() = assistedWalletManager.getGroupId(walletId)

    fun walletName() = getState().walletExtended.wallet.name

    fun walletGapLimit() = getState().walletExtended.wallet.gapLimit

    fun getRole(): AssistedWalletRole {
        return getState().role.toRole
    }

    fun getWalletName() = getState().walletExtended.wallet.name

    fun isHotWalletNeedBackup() = getState().walletExtended.wallet.needBackup

    fun isSignerDeleted() = getState().signers.firstOrNull()?.type == SignerType.UNKNOWN

    fun getTransactions() = getState().transactions

    fun isGroupSandboxWallet() = getState().isGroupSandboxWallet

    @Keep
    enum class UpdateAction {
        NAME, GAP_LIMIT
    }
}