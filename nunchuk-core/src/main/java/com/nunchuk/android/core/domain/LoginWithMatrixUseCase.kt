package com.nunchuk.android.core.domain

import com.nunchuk.android.core.matrix.MatrixInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

interface LoginWithMatrixUseCase {
    fun execute(userName: String, password: String, encryptedDeviceId: String): Flow<Session>
}

internal class LoginWithMatrixUseCaseImpl @Inject constructor(
    private val interceptor: MatrixInterceptor
) : LoginWithMatrixUseCase {

    override fun execute(userName: String, password: String, encryptedDeviceId: String) = interceptor.login(
        username = userName,
        password = password,
        encryptedDeviceId = encryptedDeviceId
    ).flowOn(Dispatchers.IO)

}