package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings

interface GetAppSettingsUseCase {
    fun execute(): AppSettings
}