package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetLocalCurrencyUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<String, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: String) {
        settingRepository.setLocalCurrency(parameters)
    }
}