package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.entities.DeveloperSetting
import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface UpdateDeveloperSettingUseCase {
    fun execute(developerSetting: DeveloperSetting): Flow<DeveloperSetting>
}

internal class UpdateDeveloperSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : UpdateDeveloperSettingUseCase {

    override fun execute(developerSetting: DeveloperSetting) = flow {
        ncSharedPreferences.developerSetting = gson.toJson(developerSetting)
        emit(
            gson.fromJson(ncSharedPreferences.developerSetting, DeveloperSetting::class.java) as DeveloperSetting
        )
    }
}