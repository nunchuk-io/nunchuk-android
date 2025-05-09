package com.nunchuk.android.usecase.settings

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetCustomExplorerUrlUseCase @Inject constructor(
    private val repository: SettingRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<String, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: String) = repository.setCustomExplorerUrl(parameters)
} 