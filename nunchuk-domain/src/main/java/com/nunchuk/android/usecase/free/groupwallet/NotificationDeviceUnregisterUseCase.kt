package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.NotificationRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class NotificationDeviceUnregisterUseCase @Inject constructor(
    private val repository: NotificationRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<NotificationDeviceUnregisterUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        repository.deviceUnregister(parameters.token)
    }

    data class Param(val token: String)
}