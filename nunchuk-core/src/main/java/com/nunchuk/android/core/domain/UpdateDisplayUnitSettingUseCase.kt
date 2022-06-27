package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.domain.data.DisplayUnitSetting
import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface UpdateDisplayUnitSettingUseCase {
    fun execute(displayUnitSetting: DisplayUnitSetting): Flow<DisplayUnitSetting>
}

internal class UpdateDisplayUnitSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : UpdateDisplayUnitSettingUseCase {

    override fun execute(displayUnitSetting: DisplayUnitSetting) = flow {
        ncSharedPreferences.displayUnitSetting = gson.toJson(displayUnitSetting)
        emit(
            gson.fromJson(ncSharedPreferences.displayUnitSetting, DisplayUnitSetting::class.java) as DisplayUnitSetting
        )
    }
}