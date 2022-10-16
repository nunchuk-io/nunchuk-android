package com.nunchuk.android.core.domain

import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetTurnOnNotificationStoreUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val ncDataStore: NcDataStore
) : UseCase<Unit, Boolean>(dispatcher) {
    override suspend fun execute(parameters: Unit): Boolean {
        return ncDataStore.turnOnNotificationFlow.first()
    }
}