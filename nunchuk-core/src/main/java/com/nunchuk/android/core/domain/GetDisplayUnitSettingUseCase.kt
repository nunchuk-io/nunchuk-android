package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.entities.DisplayUnitSetting
import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetDisplayUnitSettingUseCase {
    fun execute(): Flow<DisplayUnitSetting>
}

internal class GetDisplayUnitSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : GetDisplayUnitSettingUseCase {

    override fun execute() = gson.fromJson(
        ncSharedPreferences.displayUnitSetting,
        DisplayUnitSetting::class.java
    )?.let {
        flow { emit(it) }
    } ?: flow {
        emit(
            DisplayUnitSetting(
                useBTC = true,
                showBTCPrecision = true,
                useSAT = false
            )
        )
    }

}
