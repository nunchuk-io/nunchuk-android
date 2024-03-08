package com.nunchuk.android.core.profile

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOnBoardUseCase @Inject constructor(
    private val repository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : FlowUseCase<Unit, Boolean>(dispatcher) {
    override fun execute(parameters: Unit): Flow<Boolean> {
        return repository.showOnBoard()
    }
}