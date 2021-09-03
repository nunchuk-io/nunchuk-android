package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ChangePasswordUseCase {
    fun execute(oldPassword: String, newPassword: String): Flow<Unit>
}

internal class ChangePasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
) : ChangePasswordUseCase {

    override fun execute(oldPassword: String, newPassword: String) = authRepository.changePassword(
        oldPassword = oldPassword,
        newPassword = newPassword
    )

}