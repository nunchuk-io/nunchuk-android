package com.nunchuk.android.core.profile

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

class MarkOnBoardUseCase @Inject constructor(
    private val repository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<Unit, Unit>(dispatcher) {
    override suspend fun execute(parameters: Unit) {
        repository.markOnboardDone()
    }
}