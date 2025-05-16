package com.nunchuk.android.auth.domain

import com.nunchuk.android.core.constants.Constants.MAIN_NET_HOST
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetElectrumServersUseCase
import com.nunchuk.android.core.domain.GetLocalElectrumServersUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class AutoSelectElectrumSeverUseCase @Inject constructor(
    private val getElectrumServersUseCase: GetElectrumServersUseCase,
    private val getLocalElectrumServersUseCase: GetLocalElectrumServersUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<Unit, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Unit) {
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
}