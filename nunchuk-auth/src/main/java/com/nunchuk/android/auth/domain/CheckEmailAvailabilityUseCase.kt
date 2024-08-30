package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.auth.domain.model.EmailAvailability
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckEmailAvailabilityUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<String, EmailAvailability>(dispatcher) {
    override suspend fun execute(parameters: String): EmailAvailability {
        return authRepository.checkAvailableEmail(parameters)
    }
}