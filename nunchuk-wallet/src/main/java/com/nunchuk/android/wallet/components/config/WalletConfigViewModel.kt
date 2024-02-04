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

import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesDeleteAssistedWalletUseCase
import com.nunchuk.android.core.domain.membership.DeleteAssistedWalletUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.joinKeys
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.usecase.GetTransactionHistoryUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.membership.ExportCoinControlBIP329UseCase
import com.nunchuk.android.usecase.membership.ExportTxCoinControlUseCase
import com.nunchuk.android.usecase.membership.ForceRefreshWalletUseCase
import com.nunchuk.android.usecase.membership.ImportCoinControlBIP329UseCase
import com.nunchuk.android.usecase.membership.ImportTxCoinControlUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.retrieveInfo
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
) : NunchukViewModel<WalletConfigState, WalletConfigEvent>() {

    private val walletId: String
        get() = savedStateHandle.get<String>("EXTRA_WALLET_ID").orEmpty()

    private val contacts = getContactsUseCase.execute()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        getWalletDetails()
        getUserRole()
        getWalletAlias()
    }

    private fun getWalletAlias() {
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).mapNotNull { wallets ->
                wallets.getOrElse { emptyList() }.find { it.localId == walletId }
            }.collect {
                updateState { copy(alias = it.alias) }
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
                    )
                }
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (isAssistedWallet() && it.walletExtended.wallet.balance.pureBTC() == 0.0) {
                        getTransactionHistory()
                    }
                    updateState {
                        copy(
                            walletExtended = it.walletExtended,
                            signers = it.signers,
                            isAssistedWallet = it.isAssistedWallet
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
                    updateState { copy(role = role) }
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
            setEvent(WalletConfigEvent.Loading(true))
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    TargetAction.UPDATE_SERVER_KEY.name,
                    password
                )
            )
            setEvent(WalletConfigEvent.Loading(false))
            if (result.isSuccess) {
                setEvent(
                    WalletConfigEvent.VerifyPasswordSuccess(
                        result.getOrThrow().orEmpty(),
                        signer,
                        assistedWalletManager.getGroupId(walletId)
                    )
                )
            } else {
                setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun verifyPasswordToDeleteAssistedWallet(password: String) = viewModelScope.launch {
        setEvent(WalletConfigEvent.Loading(true))
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                TargetAction.DELETE_WALLET.name,
                password
            )
        )
        setEvent(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            val resultCalculate = calculateRequiredSignaturesDeleteAssistedWalletUseCase(
                CalculateRequiredSignaturesDeleteAssistedWalletUseCase.Param(
                    walletId = walletId,
                    groupId = assistedWalletManager.getGroupId(walletId)
                )
            )
            if (resultCalculate.isSuccess) {
                updateState { copy(verifyToken = result.getOrNull()) }
                setEvent(
                    WalletConfigEvent.CalculateRequiredSignaturesSuccess(
                        walletId = walletId,
                        requiredSignatures = resultCalculate.getOrThrow().requiredSignatures,
                        type = resultCalculate.getOrThrow().type
                    )
                )
            } else {
                setEvent(WalletConfigEvent.WalletDetailsError(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
            }
        } else {
            setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
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
            updateWalletUseCase.execute(
                newWallet,
                assistedWalletManager.isActiveAssistedWallet(walletId),
                assistedWalletManager.getGroupId(walletId),
            )
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState { copy(walletExtended = walletExtended.copy(wallet = newWallet)) }
                    if (updateAction == UpdateAction.NAME) {
                        event(UpdateNameSuccessEvent)
                    } else if (updateAction == UpdateAction.GAP_LIMIT) {
                        event(WalletConfigEvent.UpdateGapLimitSuccessEvent)
                    }
                }
        }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            setEvent(WalletConfigEvent.Loading(true))
            getTransactionHistoryUseCase.execute(walletId).flowOn(Dispatchers.IO)
                .collect { transations ->
                    setEvent(WalletConfigEvent.Loading(false))
                    val isPendingTransactionExisting = transations.any { it.status.isPending() }
                    updateState { copy(isShowDeleteAssistedWallet = isPendingTransactionExisting.not()) }
                }
        }
    }

    private fun showError(t: Throwable) {
        event(WalletConfigEvent.WalletDetailsError(t.message.orUnknownError()))
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
                        if (isAssistedWallet()) {
                            setEvent(WalletConfigEvent.DeleteAssistedWalletSuccess)
                        } else {
                            event(WalletConfigEvent.DeleteWalletSuccess)
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
        setEvent(WalletConfigEvent.Loading(true))
        val result = forceRefreshWalletUseCase(walletId)
        delay(3000L)
        setEvent(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            setEvent(WalletConfigEvent.ForceRefreshWalletSuccess)
        } else {
            setEvent(WalletConfigEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
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
                is Result.Success -> event(WalletConfigEvent.UploadWalletConfigEvent(filePath))
                is Result.Error -> showError(event.exception)
            }
        }
    }

    fun deleteAssistedWallet(
        signatures: HashMap<String, String>,
        securityQuestionToken: String,
    ) = viewModelScope.launch {
        val state = getState()
        if (state.verifyToken == null) return@launch
        setEvent(WalletConfigEvent.Loading(true))
        val result = deleteAssistedWalletUseCase(
            DeleteAssistedWalletUseCase.Param(
                signatures = signatures,
                verifyToken = state.verifyToken,
                securityQuestionToken = securityQuestionToken,
                walletId = walletId,
                groupId = assistedWalletManager.getGroupId(walletId)
            )
        )
        setEvent(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            handleDeleteWallet()
        } else {
            setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun importCoinControlNunchuk(filePath: String) = viewModelScope.launch {
        setEvent(WalletConfigEvent.Loading(true))
        val result = importTxCoinControlUseCase(
            ImportTxCoinControlUseCase.Param(
                walletId = walletId,
                data = filePath,
                force = true
            )
        )
        setEvent(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            setEvent(WalletConfigEvent.ImportTxCoinControlSuccess)
        } else {
            setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
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
                    setEvent(WalletConfigEvent.ExportTxCoinControlSuccess(event.data))
                } else {
                    setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }

            is Result.Error -> showError(event.exception)
        }
    }

    fun importCoinControlBIP329(filePath: String) = viewModelScope.launch {
        setEvent(WalletConfigEvent.Loading(true))
        val result = importCoinControlBIP329UseCase(
            ImportCoinControlBIP329UseCase.Param(
                walletId = walletId,
                data = filePath
            )
        )
        setEvent(WalletConfigEvent.Loading(false))
        if (result.isSuccess) {
            setEvent(WalletConfigEvent.ImportTxCoinControlSuccess)
        } else {
            setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
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
                    setEvent(WalletConfigEvent.ExportTxCoinControlSuccess(event.data))
                } else {
                    setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }

            is Result.Error -> showError(event.exception)
        }
    }


    private fun isPrimaryKey(id: String) =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && accountManager.getPrimaryKeyInfo()?.xfp == id

    fun isAssistedWallet() = assistedWalletManager.isActiveAssistedWallet(walletId)

    fun isShowDeleteWallet() = getState().isShowDeleteAssistedWallet || isAssistedWallet().not()

    fun isInactiveAssistedWallet() = assistedWalletManager.isInactiveAssistedWallet(walletId)

    fun getGroupId() = assistedWalletManager.getGroupId(walletId)

    fun walletName() = getState().walletExtended.wallet.name

    fun walletGapLimit() = getState().walletExtended.wallet.gapLimit

    fun getRole(): AssistedWalletRole {
        return getState().role.toRole
    }

    fun isEditableWalletName() =
        getGroupId().isNullOrEmpty() || getState().role.toRole == AssistedWalletRole.MASTER

    override val initialState: WalletConfigState
        get() = WalletConfigState()

    @Keep
    enum class UpdateAction {
        NAME, GAP_LIMIT
    }
}