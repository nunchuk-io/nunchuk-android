package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

internal class ResendVerifyNewDeviceCodeUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val authRepository: AuthRepository
) : UseCase<ResendVerifyNewDeviceCodeUseCase.Data, Unit>(dispatcher) {

    override suspend fun execute(parameters: Data) {
        authRepository.resendVerifyCode(
            email = parameters.email, loginHalfToken = parameters.loginHalfToken, deviceId = parameters.deviceId
        )
    }

    class Data(val email: String, val loginHalfToken: String, val deviceId: String)

}