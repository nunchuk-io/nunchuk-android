package com.nunchuk.android.repository

import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.byzantine.SimilarGroup

interface GroupWalletRepository {
    suspend fun findSimilarGroup(groupId: String): List<SimilarGroup>
    suspend fun reuseGroupWallet(groupId: String, fromGroupId: String): DraftWallet
    suspend fun syncGroupDraftWallet(groupId: String): DraftWallet
}