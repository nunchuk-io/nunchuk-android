package com.nunchuk.android.repository

import com.nunchuk.android.model.MemberSubscription
import com.nunchuk.android.model.MembershipStepInfo
import kotlinx.coroutines.flow.Flow

interface MembershipRepository {
    fun getSteps(): Flow<List<MembershipStepInfo>>
    suspend fun saveStepInfo(info: MembershipStepInfo)
    suspend fun deleteStepBySignerId(masterSignerId: String)
    suspend fun getSubscription() : MemberSubscription
    suspend fun restart()
}