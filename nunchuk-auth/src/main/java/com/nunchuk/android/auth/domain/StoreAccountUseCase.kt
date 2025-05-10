package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.constants.Constants.MAIN_NET_HOST
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetElectrumServersUseCase
import com.nunchuk.android.core.domain.GetLocalElectrumServersUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class StoreAccountUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val checkShowOnboardUseCase: CheckShowOnboardUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val getElectrumServersUseCase: GetElectrumServersUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val getLocalElectrumServersUseCase: GetLocalElectrumServersUseCase
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

        runCatching {
            val servers = getElectrumServersUseCase(Unit).getOrNull()?.mainnet?.map { it.url }.orEmpty()
            val localServers = getLocalElectrumServersUseCase(Unit).map { it.getOrThrow().map { it.url } }.first()
            val appSettings = getAppSettingUseCase(Unit).getOrThrow()
            val currentMainnetServer = appSettings.mainnetServers.firstOrNull()
            val isCustomServer = !currentMainnetServer.isNullOrEmpty()
                    && currentMainnetServer != MAIN_NET_HOST
                    && (servers.indexOf(currentMainnetServer) > 0 || localServers.indexOf(currentMainnetServer) >= 0)
            if (!isCustomServer) {
                val autoServer = servers.firstOrNull() ?: MAIN_NET_HOST
                Timber.d("save autoServer: $autoServer")
                updateAppSettingUseCase(
                    appSettings.copy(
                        mainnetServers = listOf(autoServer),
                    )
                )
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