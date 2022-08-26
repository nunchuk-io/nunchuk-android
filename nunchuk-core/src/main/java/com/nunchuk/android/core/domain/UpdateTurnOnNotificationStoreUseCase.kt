package com.nunchuk.android.core.domain

import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateTurnOnNotificationStoreUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val ncDataStore: NcDataStore
) : UseCase<UpdateTurnOnNotificationStoreUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        ncDataStore.updateTurnOnNotification(parameters.isTurnOn)
    }

    class Param(val isTurnOn: Boolean)
}