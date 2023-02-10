package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.HandledEventRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class IsHandledEventUseCase @Inject constructor(
    private val repository: HandledEventRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<String, Boolean>(ioDispatcher) {
    override suspend fun execute(parameters: String): Boolean {
        return repository.isHandled(parameters)
    }
}