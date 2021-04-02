package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface ChangePasswordUseCase {
    suspend fun execute(oldPassword: String, newPassword: String): Result<Unit>
}

internal class ChangePasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase(), ChangePasswordUseCase {

    override suspend fun execute(
        oldPassword: String,
        newPassword: String
    ) = exe {
        authRepository.changePassword(
            oldPassword = oldPassword,
            newPassword = newPassword
        )
    }

}