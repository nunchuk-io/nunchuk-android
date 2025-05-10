package com.nunchuk.android.core.domain.settings

import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetCustomExplorerUrlUseCase @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<SetCustomExplorerUrlUseCase.Params, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Params) {
        val appSetting = getAppSettingUseCase(Unit).getOrThrow()
        val newAppSetting = when(parameters.chain) {
            Chain.MAIN -> appSetting.copy(mainnetExplorerHost = parameters.url)
            Chain.TESTNET -> appSetting.copy(testExplorerHost = parameters.url)
            Chain.SIGNET -> appSetting.copy(signetExplorerHost = parameters.url)
            else -> appSetting
        }

        updateAppSettingUseCase(newAppSetting)
            .getOrThrow()
    }

    data class Params(
        val chain: Chain,
        val url: String
    )
} 