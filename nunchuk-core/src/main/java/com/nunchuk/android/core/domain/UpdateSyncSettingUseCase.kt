package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.domain.data.SyncSetting
import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface UpdateSyncSettingUseCase {
    fun execute(syncSetting: SyncSetting): Flow<SyncSetting>
}

internal class UpdateSyncSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : UpdateSyncSettingUseCase {

    override fun execute(syncSetting: SyncSetting) = flow {
        ncSharedPreferences.syncSetting = gson.toJson(syncSetting)
        emit(
            gson.fromJson(ncSharedPreferences.syncSetting, SyncSetting::class.java) as SyncSetting
        )
    }
}