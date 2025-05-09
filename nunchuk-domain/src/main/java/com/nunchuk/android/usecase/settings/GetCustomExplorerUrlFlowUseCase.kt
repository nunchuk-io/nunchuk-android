package com.nunchuk.android.usecase.settings

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetCustomExplorerUrlFlowUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, String>(ioDispatcher) {
    override fun execute(parameters: Unit) = settingRepository.customExplorerUrl
} 