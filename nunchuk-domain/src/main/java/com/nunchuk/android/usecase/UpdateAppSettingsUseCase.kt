package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings

interface UpdateAppSettingsUseCase {
    fun execute(appSettings: AppSettings): AppSettings
}