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

package com.nunchuk.android.wallet.components.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.fromCurrencyToBTC
import com.nunchuk.android.core.util.getBtcSat
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.usecase.GetTransactionHistoryUseCase
import com.nunchuk.android.usecase.membership.GetServerTransactionUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class SearchTransactionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getServerTransactionUseCase: GetServerTransactionUseCase,
    private val getTransactionHistoryUseCase: GetTransactionHistoryUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val args: SearchTransactionFragmentArgs =
        SearchTransactionFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<SearchTransactionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(SearchTransactionState())
    val state = _state.asStateFlow()

    private var allTransactionExtends = mutableListOf<ExtendedTransaction>()
    private val isAssistedWallet by lazy { assistedWalletManager.isActiveAssistedWallet(args.walletId) }
    private val queryState = MutableStateFlow("")

    private val defaultFilter = CoinFilterUiState()
    val filter = savedStateHandle.getStateFlow(KEY_FILTER, defaultFilter)

    fun updateFilter(filter: CoinFilterUiState) {
        savedStateHandle[KEY_FILTER] = filter
        viewModelScope.launch(ioDispatcher) {
            handleSearch(queryState.value)
        }
    }

    init {
        getTransactionHistory()

        queryState.debounce(300)
            .flowOn(ioDispatcher)
            .onEach {
                handleSearch(it)
            }.launchIn(viewModelScope)
    }

    private fun getTransactionHistory() {
        viewModelScope.launch {
            getTransactionHistoryUseCase.execute(args.walletId)
                .map { transactions ->
                    transactions.sortedWith(
                        compareBy(Transaction::status).thenByDescending(
                            Transaction::blockTime
                        )
                    )
                    val serverTransactions = hashMapOf<String, ServerTransaction?>()
                    allTransactionExtends.addAll(transactions.map {
                        it.toTransactionExtended(serverTransactions)
                    })
                }
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(SearchTransactionEvent.Error(it.message.orUnknownError())) }
                .collect {}
        }
    }

    private suspend fun Transaction.toTransactionExtended(serverTransactions: HashMap<String, ServerTransaction?>): ExtendedTransaction {
        if (isAssistedWallet && this.signers.any { it.value } && this.status.isPending()) {
            if (serverTransactions.contains(this.txId).not()) {
                serverTransactions[this.txId] = getServerTransactionUseCase(
                    GetServerTransactionUseCase.Param(
                        groupId = assistedWalletManager.getGroupId(args.walletId),
                        walletId = args.walletId,
                        txId = this.txId
                    )
                ).getOrNull()?.serverTransaction
            }
            return ExtendedTransaction(
                transaction = this,
                serverTransaction = serverTransactions[this.txId]
            )
        }
        return ExtendedTransaction(transaction = this)
    }

    fun search(query: String) {
        queryState.value = query
    }

    private fun handleSearch(query: String) {
        if (query.isEmpty() && isFiltering.not()) {
            _state.update { it.copy(query = query, transactions = emptyList()) }
        } else {
            val filter = filter.value
            val endTimeInSeconds =
                if (filter.endTime > 0L) filter.endTime / 1000L else Long.MAX_VALUE
            val startTimeInSeconds = filter.startTime / 1000L
            val lowCaseQuery = query.lowercase()

            val comparator = if (filter.isDescending)
                compareByDescending<ExtendedTransaction> { it.transaction.totalAmount.value }.thenBy { it.transaction.blockTime }
            else compareBy<ExtendedTransaction> { it.transaction.totalAmount.value }.thenBy { it.transaction.blockTime }

            val minValue = filter.min.toDoubleOrNull() ?: 0.0
            val minSat =
                if (filter.isMinBtc) minValue.getBtcSat() else minValue.fromCurrencyToBTC()
                    .toAmount().value

            val maxValue = filter.max.toDoubleOrNull() ?: Long.MAX_VALUE.toDouble()
            val maxSat =
                if (filter.isMaxBtc) maxValue.getBtcSat() else maxValue.fromCurrencyToBTC()
                    .toAmount().value
            val transactions = allTransactionExtends
                .asSequence()
                .filter {
                    lowCaseQuery.isEmpty()
                            || it.transaction.memo.contains(lowCaseQuery)
                            || (it.transaction.isReceive && it.transaction.receiveOutputs.firstOrNull()?.first.orEmpty().lowercase().contains(lowCaseQuery))
                            || (it.transaction.isReceive.not() && it.transaction.outputs.firstOrNull()?.first.orEmpty().lowercase().contains(lowCaseQuery))
                            || it.transaction.totalAmount.value.toString().contains(lowCaseQuery)
                }
                .filter { it.transaction.totalAmount.value in minSat..maxSat }
                .filter { it.transaction.blockTime in startTimeInSeconds..endTimeInSeconds }
                .sortedWith(comparator)
                .toList()
            _state.update { it.copy(query = query, transactions = transactions) }
        }
    }

    val isFiltering: Boolean
        get() = defaultFilter != filter.value

    val isFilteringOrSearch: Boolean
        get() = defaultFilter != filter.value || queryState.value.isNotEmpty()

    companion object {
        private const val KEY_FILTER = "filter"
    }
}