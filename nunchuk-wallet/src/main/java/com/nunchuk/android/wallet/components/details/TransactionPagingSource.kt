/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.usecase.membership.GetServerTransactionUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import javax.inject.Inject

internal const val STARTING_PAGE = 1
internal const val PAGE_SIZE = 100

class TransactionPagingSource @Inject constructor(
    private val transactions: List<Transaction>,
    private val getServerTransactionUseCase: GetServerTransactionUseCase,
    private val walletId: String,
    private val isAssistedWallet: Boolean,
    private val serverTransactions: MutableMap<String, ServerTransaction?>
) : PagingSource<Int, ExtendedTransaction>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ExtendedTransaction> {
        return try {
            val position = params.key ?: STARTING_PAGE
            val fromIndex = (position - 1) * PAGE_SIZE
            val toIndex = (position * PAGE_SIZE).coerceAtMost(transactions.size)
            val data = transactions.subList(fromIndex, toIndex).map { transaction ->
                if (isAssistedWallet && transaction.signers.any { it.value } && transaction.status.isPending()) {
                    if (serverTransactions.contains(transaction.txId).not()) {
                        serverTransactions[transaction.txId] = getServerTransactionUseCase(
                            GetServerTransactionUseCase.Param(
                                walletId,
                                transaction.txId
                            )
                        ).getOrNull()?.serverTransaction
                    }
                    return@map ExtendedTransaction(
                        transaction = transaction,
                        serverTransaction = serverTransactions[transaction.txId]
                    )
                }
                return@map ExtendedTransaction(transaction = transaction)
            }
            val hasNextPage = ((position * PAGE_SIZE) < transactions.size)
            LoadResult.Page(
                data = data,
                prevKey = null,
                nextKey = if (data.isEmpty() || !hasNextPage) null else position + 1
            )
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
            return LoadResult.Error(t)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ExtendedTransaction>) =
        state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }

}