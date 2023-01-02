package com.nunchuk.android.core.domain.settings

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChainSettingFlowUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val settingRepository: SettingRepository,
) : FlowUseCase<Unit, Chain>(dispatcher) {
    override fun execute(parameters: Unit): Flow<Chain> = settingRepository.chain
}