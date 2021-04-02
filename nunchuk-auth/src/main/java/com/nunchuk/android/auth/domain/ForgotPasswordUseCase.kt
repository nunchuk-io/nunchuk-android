package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface ForgotPasswordUseCase {
    suspend fun execute(email: String): Result<Unit>
}

internal class ForgotPasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase(), ForgotPasswordUseCase {

    override suspend fun execute(email: String) = exe {
        authRepository.forgotPassword(email = email)
    }
}