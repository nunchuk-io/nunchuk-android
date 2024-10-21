package com.nunchuk.android.auth.domain

import com.nunchuk.android.core.profile.SetOnBoardUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckShowOnboardUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val setOnBoardUseCase: SetOnBoardUseCase,
) : UseCase<Unit, Unit>(dispatcher) {
    override suspend fun execute(parameters: Unit) {
        setOnBoardUseCase(false)
    }
}