package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.domain.data.SyncSetting
import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetSyncSettingUseCase {
    fun execute(): Flow<SyncSetting>
}

internal class GetSyncSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : GetSyncSettingUseCase {

    override fun execute() = gson.fromJson(
        ncSharedPreferences.syncSetting,
        SyncSetting::class.java
    )?.let {
        flow { emit(it) }
    } ?: flow {
        emit(SyncSetting(enable = false))
    }
}
