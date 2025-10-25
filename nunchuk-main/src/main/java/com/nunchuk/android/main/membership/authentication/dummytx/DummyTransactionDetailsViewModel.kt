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

package com.nunchuk.android.main.membership.authentication.dummytx

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.ParseSignerStringUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinsGroup
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.transaction.components.details.TransactionMiniscriptUiState
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.IsScriptNodeSatisfiableUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.membership.GetDummyTxFromPsbtByteArrayUseCase
import com.nunchuk.android.usecase.transaction.GetTransactionSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
internal class DummyTransactionDetailsViewModel @Inject constructor(
    private val getDummyTxFromPsbtByteArrayUseCase: GetDummyTxFromPsbtByteArrayUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val application: Application,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
    private val parseSignerStringUseCase: ParseSignerStringUseCase,
    private val isScriptNodeSatisfiableUseCase: IsScriptNodeSatisfiableUseCase,
    private val getChainTipUseCase: GetChainTipUseCase,
    private val getTransactionSignersUseCase: GetTransactionSignersUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val args = DummyTransactionDetailsFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val walletId = args.walletId
    private val _state = MutableStateFlow(DummyTransactionState())
    val state = _state.asStateFlow()

    private val _minscriptState = MutableStateFlow(TransactionMiniscriptUiState())
    val miniscriptState = _minscriptState.asStateFlow()

    private val _event = MutableSharedFlow<DummyTransactionDetailEvent>()
    val event = _event.asSharedFlow()

    private val satisfiableMap: MutableMap<String, Boolean> = mutableMapOf()
    private val coinIdsGroups: MutableMap<String, CoinsGroup> = mutableMapOf()

    init {
        startChainTipUpdater()
    }

    fun importTransactionViaFile(walletId: String, uri: Uri) {
        viewModelScope.launch {
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(true))
            val bytes = withContext(ioDispatcher) {
                getFileFromUri(
                    application.contentResolver,
                    uri,
                    application.cacheDir
                )?.let { file ->
                    File(file.absolutePath).readBytes()
                }
            } ?: return@launch
            val result = getDummyTxFromPsbtByteArrayUseCase(
                GetDummyTxFromPsbtByteArrayUseCase.Param(
                    walletId,
                    bytes
                )
            )
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(false))
            if (result.isSuccess) {
                val transaction = result.getOrThrow()
                _event.emit(DummyTransactionDetailEvent.ImportTransactionSuccess(transaction))
            } else {
                _event.emit(DummyTransactionDetailEvent.TransactionError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }


    fun exportTransactionToFile(dataToSign: String) {
        viewModelScope.launch {
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(true))
            when (val result = createShareFileUseCase.execute("dummy.psbt")) {
                is Result.Success -> exportTransaction(result.data, dataToSign)
                is Result.Error -> _event.emit(DummyTransactionDetailEvent.TransactionError(result.exception.messageOrUnknownError()))
            }
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(false))
        }
    }

    private fun exportTransaction(filePath: String, dataToSign: String) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(ioDispatcher) {
                    FileOutputStream(filePath).use {
                        it.write(dataToSign.toByteArray(Charsets.UTF_8))
                    }
                }
            }
            if (result.isSuccess) {
                _event.emit(DummyTransactionDetailEvent.ExportToFileSuccess(filePath))
            } else {
                _event.emit(DummyTransactionDetailEvent.TransactionError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun saveLocalFile(dataToSign: String) {
        viewModelScope.launch {
            val result = saveLocalFileUseCase(
                SaveLocalFileUseCase.Params(
                    fileName = "dummy.psbt",
                    fileContent = dataToSign
                )
            )
            _event.emit(DummyTransactionDetailEvent.SaveLocalFile(result.isSuccess))
        }
    }

    fun handleViewMoreEvent() {
        _state.update { it.copy(viewMore = it.viewMore.not()) }
    }

    // Miniscript methods
    private fun clearMiniscriptMaps() {
        satisfiableMap.clear()
    }

    private suspend fun getMiniscriptInfo(
        wallet: Wallet,
        transaction: Transaction,
    ) {
        clearMiniscriptMaps()
        getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
            val signerMap = parseSignersFromScriptNode(result.scriptNode, transaction)
            _minscriptState.update {
                it.copy(
                    scriptNode = result.scriptNode,
                    satisfiableMap = satisfiableMap,
                    coinGroups = coinIdsGroups,
                    signerMap = signerMap
                )
            }
        }
    }

    private fun startChainTipUpdater() {
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

    private suspend fun parseSignersFromScriptNode(node: ScriptNode, transaction: Transaction): Map<String, SignerModel> {
        if (!satisfiableMap.containsKey(node.idString)) {
            satisfiableMap[node.idString] = isScriptNodeSatisfiableUseCase(
                IsScriptNodeSatisfiableUseCase.Params(
                    nodeId = node.id.toIntArray(),
                    walletId = walletId,
                    psbt = transaction.psbt
                )
            ).getOrDefault(false)
        }

        if (satisfiableMap[node.idString] == false) {
            node.subs.forEach { subNode ->
                satisfiableMap[subNode.idString] = false
            }
        }

        // Special case for ANDOR node
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

    fun isMiniscriptWallet(): Boolean {
        return _minscriptState.value.isMiniscriptWallet
    }

    fun loadWallet(transaction: Transaction) {
        if (walletId.isEmpty()) return
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                _minscriptState.update { it.copy(isMiniscriptWallet = wallet.miniscript.isNotEmpty()) }
                if (wallet.miniscript.isNotEmpty()) {
                    getMiniscriptInfo(
                        wallet = wallet,
                        transaction = transaction
                    )
                }
            }
        }
    }
}

sealed class DummyTransactionDetailEvent {
    data class LoadingEvent(val isLoading: Boolean) : DummyTransactionDetailEvent()
    data class ImportTransactionSuccess(val transaction: Transaction?) :
        DummyTransactionDetailEvent()

    data class ExportToFileSuccess(val filePath: String) : DummyTransactionDetailEvent()
    data class TransactionError(val error: String) : DummyTransactionDetailEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : DummyTransactionDetailEvent()
}