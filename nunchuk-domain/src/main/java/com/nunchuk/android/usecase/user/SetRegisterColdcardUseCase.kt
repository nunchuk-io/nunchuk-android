package com.nunchuk.android.usecase.user

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetRegisterColdcardUseCase @Inject constructor(
    private val repository: MembershipRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<Boolean, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Boolean) = repository.setRegisterColdcard(parameters)
}