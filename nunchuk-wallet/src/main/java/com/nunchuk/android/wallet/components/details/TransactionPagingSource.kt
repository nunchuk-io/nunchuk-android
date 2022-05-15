package com.nunchuk.android.wallet.components.details

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.utils.CrashlyticsReporter
import javax.inject.Inject

internal const val STARTING_PAGE = 1
internal const val PAGE_SIZE = 100

class TransactionPagingSource @Inject constructor(
    private val transactions: List<Transaction>
) : PagingSource<Int, Transaction>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        return try {
            val position = params.key ?: STARTING_PAGE
            val data = transactions.subList(((position - 1) * PAGE_SIZE), (position * PAGE_SIZE).coerceAtMost(transactions.size))
            val hasNextPage = ((position + 1) * PAGE_SIZE < transactions.size)
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

    override fun getRefreshKey(state: PagingState<Int, Transaction>) = state.anchorPosition?.let {
        state.closestPageToPosition(it)?.prevKey?.plus(1) ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
    }

}