package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.network.HeaderProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

interface MatrixInterceptor {
    fun login(username: String, password: String, encryptedDeviceId: String): Flow<Session>
}

internal class MatrixInterceptorImpl @Inject constructor(
    matrix: Matrix,
    private val matrixProvider: MatrixProvider,
    private val headerProvider: HeaderProvider,
    private val sessionHolder: SessionHolder
) : MatrixInterceptor {

    private var authenticationService = matrix.authenticationService()

    override fun login(username: String, password: String, encryptedDeviceId: String) = flow {
        emit(
            authenticationService
                .directAuthentication(
                    homeServerConnectionConfig = matrixProvider.getServerConfig(),
                    matrixId = username,
                    password = password,
                    initialDeviceName = headerProvider.getDeviceName(),
                    deviceId = encryptedDeviceId
                ).apply {
                    authenticationService.reset()
                    sessionHolder.storeActiveSession(this)
                    MatrixEvenBus.instance.publish(MatrixEvent.SignedInEvent(this))
                })
    }

}