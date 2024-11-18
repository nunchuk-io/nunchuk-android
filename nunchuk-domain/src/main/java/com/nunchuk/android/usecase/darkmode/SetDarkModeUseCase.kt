package com.nunchuk.android.usecase.darkmode

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetDarkModeUseCase @Inject constructor(
    private val repository: SettingRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<Boolean?, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Boolean?) {
        repository.setDarkMode(parameters)
    }
}