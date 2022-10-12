package com.nunchuk.android.core.repository

import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class SettingRepositoryImpl @Inject constructor(
    private val ncDataStore: NcDataStore
): SettingRepository {
    override val syncEnable: Flow<Boolean>
        get() = ncDataStore.syncEnableFlow

    override suspend fun setSyncEnable(isEnable: Boolean) {
        ncDataStore.setSyncEnable(isEnable)
    }
}