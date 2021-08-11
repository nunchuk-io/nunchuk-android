package com.nunchuk.android.auth.domain

import com.nunchuk.android.core.matrix.MatrixInterceptor
import com.nunchuk.android.core.matrix.SessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

interface LoginWithMatrixUseCase {
    fun execute(userName: String, password: String): Flow<Session>
}

internal class LoginWithMatrixUseCaseImpl @Inject constructor(
    private val interceptor: MatrixInterceptor
) : LoginWithMatrixUseCase {

    override fun execute(
        userName: String,
        password: String
    ) = interceptor.login(userName, password).map {
        it.apply {
            SessionHolder.currentSession = this
            open()
            startSync(true)
        }
    }

}