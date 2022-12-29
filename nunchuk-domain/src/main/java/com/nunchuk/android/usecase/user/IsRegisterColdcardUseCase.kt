package com.nunchuk.android.usecase.user

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.MembershipRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsRegisterColdcardUseCase @Inject constructor(
    private val repository: MembershipRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, Boolean>(ioDispatcher) {
    override fun execute(parameters: Unit): Flow<Boolean> = repository.isRegisterColdcard()
}