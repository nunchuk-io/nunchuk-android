package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.NotificationRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

class NotificationDeviceRegisterUseCase @Inject constructor(
    private val repository: NotificationRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<NotificationDeviceRegisterUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        Timber.tag("notification-service-fcm").e("registering device with token: ${parameters.token}")
        repository.deviceRegister(parameters.token)
    }

    data class Param(val token: String)
}