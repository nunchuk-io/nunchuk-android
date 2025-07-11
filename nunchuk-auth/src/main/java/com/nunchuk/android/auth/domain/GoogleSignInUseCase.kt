package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: AuthRepository,
    private val storeAccountUseCase: StoreAccountUseCase
) : UseCase<String, AccountInfo>(ioDispatcher) {

    override suspend fun execute(parameters: String): AccountInfo {
        val userResponse = repository.googleSignIn(parameters)
        return storeAccountUseCase(
            StoreAccountUseCase.Param(
                "",
                userResponse,
                staySignedIn = true,
                fetchUserInfo = true,
                loginType = SignInMode.EMAIL
            )
        ).getOrThrow()
    }
}