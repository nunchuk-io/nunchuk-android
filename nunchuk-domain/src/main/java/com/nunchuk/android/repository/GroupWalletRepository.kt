package com.nunchuk.android.repository

import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.byzantine.SimilarGroup
import kotlinx.coroutines.flow.Flow

interface GroupWalletRepository {
    suspend fun findSimilarGroup(groupId: String): List<SimilarGroup>
    suspend fun reuseGroupWallet(groupId: String, fromGroupId: String): DraftWallet
    suspend fun syncGroupDraftWallet(groupId: String): DraftWallet
    fun getWalletHealthStatus(groupId: String, walletId: String): Flow<List<KeyHealthStatus>>
    suspend fun getWalletHealthStatusRemote(groupId: String, walletId: String): List<KeyHealthStatus>
    suspend fun requestHealthCheck(groupId: String, walletId: String, xfp: String)
    suspend fun healthCheck(groupId: String, walletId: String, xfp: String, draft: Boolean): DummyTransactionPayload
}