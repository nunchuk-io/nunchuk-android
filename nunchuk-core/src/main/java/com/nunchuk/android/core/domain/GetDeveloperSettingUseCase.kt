package com.nunchuk.android.core.domain

import com.google.gson.Gson
import com.nunchuk.android.core.domain.data.DeveloperSetting
import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface GetDeveloperSettingUseCase {
    fun execute(): Flow<DeveloperSetting>
}

internal class GetDeveloperSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : GetDeveloperSettingUseCase {

    override fun execute() = gson.fromJson(
        ncSharedPreferences.developerSetting,
        DeveloperSetting::class.java
    )?.let {
        flow { emit(it) }.flowOn(Dispatchers.IO)
    } ?: flow {
        emit(DeveloperSetting(debugMode = false))
    }.flowOn(Dispatchers.IO)

}
