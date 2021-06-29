package com.nunchuk.android.auth.domain

import com.nunchuk.android.core.matrix.MatrixInterceptor
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface LoginWithMatrixUseCase {
    suspend fun execute(userName: String, password: String): Result<Unit>
}

internal class LoginWithMatrixUseCaseImpl @Inject constructor(
    private val interceptor: MatrixInterceptor
) : BaseUseCase(), LoginWithMatrixUseCase {

    override suspend fun execute(userName: String, password: String) = exe {
        SessionHolder.currentSession = interceptor.login(userName, password).apply {
            open()
            startSync(true)
        }
    }

}