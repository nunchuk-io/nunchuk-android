package com.nunchuk.android.repository

import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.MembershipStep
import kotlinx.coroutines.flow.Flow

interface KeyRepository {
    fun uploadBackupKey(
        step: MembershipStep,
        keyName: String,
        keyType: String,
        xfp: String,
        filePath: String,
        isAddNewKey: Boolean
    ): Flow<KeyUpload>

    suspend fun setKeyVerified(
        masterSignerId: String
    )
}