package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.HandledEventRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SaveHandledEventUseCase @Inject constructor(
    private val repository: HandledEventRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<String, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: String) {
        repository.save(parameters)
    }
}