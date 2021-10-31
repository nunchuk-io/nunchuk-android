package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetAppSettingUseCase {
    fun execute(): Flow<AppSettings>
}

internal class GetAppSettingUseCaseUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : GetAppSettingUseCase {

    override fun execute() = flow {
        emit(
            gson.fromJson(ncSharedPreferences.appSettings, AppSettings::class.java)
        )
    }
}
