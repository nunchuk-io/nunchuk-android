package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class StoreAccountUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val checkShowOnboardUseCase: CheckShowOnboardUseCase,
) : UseCase<StoreAccountUseCase.Param, AccountInfo>(ioDispatcher) {

    override suspend fun execute(parameters: Param): AccountInfo {
        val account = accountManager.getAccount().copy(
            email = parameters.email,
            token = parameters.response.tokenId,
            activated = true,
            staySignedIn = parameters.staySignedIn,
            deviceId = parameters.response.deviceId,
        )
        accountManager.storeAccount(account)

        if (parameters.fetchUserInfo) {
            runCatching {
                getUserProfileUseCase(Unit)
                checkShowOnboardUseCase(Unit)
            }
        }

        return accountManager.getAccount()

    }

    class Param(
        val email: String,
        val response: UserTokenResponse,
        val staySignedIn: Boolean,
        val fetchUserInfo: Boolean
    )
}