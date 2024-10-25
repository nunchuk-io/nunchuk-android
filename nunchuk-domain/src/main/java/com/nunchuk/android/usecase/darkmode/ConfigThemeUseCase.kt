package com.nunchuk.android.usecase.darkmode

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConfigThemeUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FlowUseCase<Unit, Unit>(ioDispatcher) {
    override fun execute(parameters: Unit): Flow<Unit> = settingRepository.getDarkMode().map {
        when (it) {
            true -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            false -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            null -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }
}