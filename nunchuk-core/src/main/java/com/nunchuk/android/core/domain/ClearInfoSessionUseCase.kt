package com.nunchuk.android.core.domain

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ClearInfoSessionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val sessionHolder: SessionHolder,
    private val accountManager: AccountManager,
    private val singInModeHolder: SignInModeHolder,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder
) : UseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(parameters: Unit) {
        sessionHolder.clearActiveSession()
        accountManager.signOut()
        singInModeHolder.clear()
        primaryKeySignerInfoHolder.clear()
    }
}