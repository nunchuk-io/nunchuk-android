package com.nunchuk.android.usecase.darkmode

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ThemeMode
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDarkModeUseCase @Inject constructor(
    private val repository: SettingRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, ThemeMode>(ioDispatcher) {
    override fun execute(parameters: Unit): Flow<ThemeMode> =
        repository.getDarkMode().map {
            when (it) {
                true -> ThemeMode.Dark
                false -> ThemeMode.Light
                null -> ThemeMode.System
            }
        }
}