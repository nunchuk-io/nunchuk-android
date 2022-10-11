package com.nunchuk.android.core.domain

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSyncSettingUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: SettingRepository
) : FlowUseCase<Unit, Boolean>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Boolean> {
        return repository.syncEnable
    }
}
