package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.Matrix
import timber.log.Timber
import javax.inject.Inject

class MatrixInitializer @Inject constructor(
    private val instance: Matrix,
    private val accountManager: AccountManager
) {

    fun initialize() {
        if (!accountManager.getAccount().staySignedIn) return
        val authenticationService = instance.authenticationService()
        try {
            if (authenticationService.hasAuthenticatedSessions()) {
                authenticationService
                    .getLastAuthenticatedSession()
                    ?.let(SessionHolder::storeActiveSession)
            }
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
        }
    }

    fun terminate() {
        Timber.tag("MatrixInitializer").d("staySignedIn::${accountManager.getAccount().staySignedIn}")
        if (!accountManager.getAccount().staySignedIn) {
            accountManager.signOut()
        }
    }

}