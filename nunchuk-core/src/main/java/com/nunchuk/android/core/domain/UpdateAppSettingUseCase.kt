package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface UpdateAppSettingUseCase {
    fun execute(appSettings: AppSettings): Flow<AppSettings>
}

internal class UpdateAppSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : UpdateAppSettingUseCase {

    override fun execute(appSettings: AppSettings) = flow {
        ncSharedPreferences.appSettings = gson.toJson(appSettings)
        emit(
            gson.fromJson(ncSharedPreferences.appSettings, AppSettings::class.java) as AppSettings
        )
    }
}