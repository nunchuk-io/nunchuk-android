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

package com.nunchuk.android.wallet.components.coin.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.listener.TransactionListener
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllCollectionsUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.LockCoinUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromCollectionUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromTagUseCase
import com.nunchuk.android.usecase.coin.UnLockCoinUseCase
import com.nunchuk.android.usecase.miniscript.GetSpendableNowAmountUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val lockCoinUseCase: LockCoinUseCase,
    private val unLockCoinUseCase: UnLockCoinUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val removeCoinFromTagUseCase: RemoveCoinFromTagUseCase,
    private val removeCoinFromCollectionUseCase: RemoveCoinFromCollectionUseCase,
    private val getAllCollectionsUseCase: GetAllCollectionsUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val pushEventManager: PushEventManager,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getSpendableNowAmountUseCase: GetSpendableNowAmountUseCase,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase
) : ViewModel() {
    private val walletId = savedStateHandle.get<String>("wallet_id").orEmpty()
    private val _state = MutableStateFlow(CoinListUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CoinListEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            TransactionListener.transactionUpdateFlow.sample(1000L).collect {
                if (it.walletId == walletId) {
                    getAllCoins()
                    getAllTags()
                    getAllCollections()
                    getSpendableAmount()
                }
            }
        }
        viewModelScope.launch {
            pushEventManager.event.filterIsInstance<PushEvent.CoinUpdated>()
                .filter { it.walletId == walletId }
                .collect {
                    getAllCoins()
                    getAllTags()
                    getAllCollections()
                    getSpendableAmount()
                }
        }
        refresh()
        getMiniscriptInfo()
    }

    fun refresh() {
        getAllCoins()
        getAllTags()
        getAllCollections()
        getSpendableAmount()
        _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
    }

    private fun getAllCoins() {
        viewModelScope.launch {
            _event.emit(CoinListEvent.Loading(true))
            getAllCoinUseCase(walletId).onSuccess { coins ->
                _event.emit(CoinListEvent.Loading(false))
                _state.update { state ->
                    state.copy(coins = coins)
                }
            }
        }
    }

    private fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { tags ->
                _state.update { state ->
                    state.copy(tags = tags.associateBy { it.id })
                }
            }
        }
    }

    private fun getAllCollections() {
        viewModelScope.launch {
            getAllCollectionsUseCase(walletId).onSuccess { collections ->
                _state.update { state ->
                    state.copy(collections = collections.associateBy { it.id })
                }
            }
        }
    }

    fun onLockCoin(walletId: String, selectedCoins: List<UnspentOutput>) {
        viewModelScope.launch {
            selectedCoins.asSequence().filter { it.isLocked.not() }.forEach {
                lockCoinUseCase(
                    LockCoinUseCase.Params(
                        groupId = assistedWalletManager.getGroupId(walletId),
                        walletId = walletId,
                        txId = it.txid,
                        vout = it.vout,
                        isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
                    )
                )
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinLocked)
        }
    }

    fun onUnlockCoin(
        walletId: String,
        selectedCoins: List<UnspentOutput>,
        isCreateTransaction: Boolean
    ) {
        viewModelScope.launch {
            selectedCoins.asSequence().filter { it.isLocked }.forEach {
                unLockCoinUseCase(
                    UnLockCoinUseCase.Params(
                        groupId = assistedWalletManager.getGroupId(walletId),
                        walletId = walletId,
                        txId = it.txid,
                        vout = it.vout,
                        isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
                    )
                )
            }
            if (isCreateTransaction) {
                _state.update {
                    it.copy(selectedCoins = selectedCoins.map { coin ->
                        coin.copy(
                            isLocked = false
                        )
                    }.toSet())
                }
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinUnlocked(isCreateTransaction))
        }
    }

    fun removeCoinFromTag(walletId: String, tagId: Int) = viewModelScope.launch {
        val result = removeCoinFromTagUseCase(
            RemoveCoinFromTagUseCase.Param(
                groupId = assistedWalletManager.getGroupId(walletId),
                walletId = walletId,
                tagIds = listOf(tagId),
                coins = _state.value.selectedCoins.toList(),
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinListEvent.RemoveCoinFromTagSuccess)
        } else {
            _event.emit(CoinListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun removeCoinFromCollection(walletId: String, collectionId: Int) = viewModelScope.launch {
        val result = removeCoinFromCollectionUseCase(
            RemoveCoinFromCollectionUseCase.Param(
                groupId = assistedWalletManager.getGroupId(walletId),
                walletId = walletId,
                collectionIds = listOf(collectionId),
                coins = _state.value.selectedCoins.toList(),
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinListEvent.RemoveCoinFromCollectionSuccess)
        } else {
            _event.emit(CoinListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun enableSelectMode() {
        _state.update { it.copy(mode = CoinListMode.SELECT) }
    }

    fun onSelectOrUnselectAll(isSelect: Boolean, coins: List<UnspentOutput>) {
        _state.update {
            it.copy(
                selectedCoins = if (isSelect) coins.toSet() else emptySet()
            )
        }
    }

    fun onSelectDone() {
        _state.update { it.copy(mode = CoinListMode.NONE, selectedCoins = emptySet()) }
    }

    fun onCoinSelect(coin: UnspentOutput, isSelect: Boolean) {
        val selectedCoins = state.value.selectedCoins.toMutableSet()
        if (isSelect) selectedCoins.add(coin) else selectedCoins.remove(coin)
        _state.update {
            it.copy(
                selectedCoins = selectedCoins
            )
        }
    }

    fun resetSelect() {
        _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
    }

    fun getSelectedCoins() = _state.value.selectedCoins.toList()

    fun getLockedCoins() = _state.value.coins.filter { it.isLocked }

    private fun getSpendableAmount() {
        viewModelScope.launch {
            getSpendableNowAmountUseCase(walletId).onSuccess { amount ->
                _state.update { state ->
                    state.copy(spendableAmount = amount)
                }
            }
        }
    }

    private fun getMiniscriptInfo() {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                if (wallet.miniscript.isNotEmpty()) {
                    getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
                        val warningInfo = analyzeMiniscriptForTimelocks(result.scriptNode)
                        _state.update { state ->
                            state.copy(miniscriptWarningInfo = warningInfo)
                        }
                    }
                } else {
                    _state.update { state ->
                        state.copy(miniscriptWarningInfo = null)
                    }
                }
            }
        }
    }

    private fun analyzeMiniscriptForTimelocks(scriptNode: ScriptNode): TimelockWarningInfo {
        var hasRelativeTimelock = false
        var hasAbsoluteTimelock = false

        // Use stack instead of recursion
        val stack = mutableListOf<ScriptNode>()
        stack.add(scriptNode)

        while (stack.isNotEmpty()) {
            val node = stack.removeAt(stack.size - 1)

            when (node.type) {
                ScriptNodeType.OLDER.name -> {
                    hasRelativeTimelock = true
                }

                ScriptNodeType.AFTER.name -> {
                    hasAbsoluteTimelock = true
                }
            }

            // Add sub-nodes to stack for processing
            stack.addAll(node.subs)
        }

        return TimelockWarningInfo(
            hasRelativeTimelock = hasRelativeTimelock,
            hasAbsoluteTimelock = hasAbsoluteTimelock,
        )
    }

    fun isRelativeTimelockWallet(): Boolean {
        return state.value.miniscriptWarningInfo?.hasRelativeTimelock == true
    }
}

sealed class CoinListEvent {
    data class Loading(val isLoading: Boolean) : CoinListEvent()
    data class Error(val message: String) : CoinListEvent()
    object CoinLocked : CoinListEvent()
    data class CoinUnlocked(val isCreateTransaction: Boolean) : CoinListEvent()
    object RemoveCoinFromTagSuccess : CoinListEvent()
    object RemoveCoinFromCollectionSuccess : CoinListEvent()
}