package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface ChangePasswordUseCase {
    suspend fun execute(oldPassword: String, newPassword: String, confirmPassword: String): Result<Unit>
}

internal class ChangePasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
) : ChangePasswordUseCase {

    override suspend fun execute(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) = withContext(Dispatchers.IO) {
        try {
            authRepository.changePassword(
                oldPassword = oldPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

}