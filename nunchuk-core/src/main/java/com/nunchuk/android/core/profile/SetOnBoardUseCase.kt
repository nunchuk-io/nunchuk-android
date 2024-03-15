package com.nunchuk.android.core.profile

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetOnBoardUseCase @Inject constructor(
    private val repository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<Boolean, Unit>(dispatcher) {
    override suspend fun execute(parameters: Boolean) {
        repository.setShowOnBoard(parameters)
    }
}