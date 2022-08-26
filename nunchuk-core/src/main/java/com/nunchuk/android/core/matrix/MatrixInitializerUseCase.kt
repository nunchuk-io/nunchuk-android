package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.matrix.android.sdk.api.Matrix
import javax.inject.Inject

// TODO Hai
class MatrixInitializerUseCase @Inject constructor(
    private val instance: Matrix,
    private val accountManager: AccountManager,
    private val sessionHolder: SessionHolder,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(parameters: Unit) {
        if (!accountManager.getAccount().staySignedIn) return
        if (sessionHolder.hasActiveSession()) return
        val authenticationService = instance.authenticationService()
        if (authenticationService.hasAuthenticatedSessions()) {
            authenticationService
                .getLastAuthenticatedSession()
                ?.let {
                    sessionHolder.storeActiveSession(it)
                }
        }
    }
}