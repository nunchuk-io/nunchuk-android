package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ResendPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<String, Unit>(dispatcher) {
    override suspend fun execute(parameters: String) {
        authRepository.resendPassword(parameters)
    }
}