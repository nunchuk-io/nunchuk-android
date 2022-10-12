package com.nunchuk.android.repository

import kotlinx.coroutines.flow.Flow

interface SettingRepository {
    val syncEnable: Flow<Boolean>
    suspend fun setSyncEnable(isEnable: Boolean)
}