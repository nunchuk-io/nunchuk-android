package com.nunchuk.android.core.domain.settings

import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetCustomExplorerUrlFlowUseCase @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingUseCase,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<Chain, String>(ioDispatcher) {
    override suspend fun execute(parameters: Chain): String {
        val appSetting = getAppSettingUseCase(Unit).getOrThrow()
        return when (parameters) {
            Chain.MAIN -> appSetting.mainnetExplorerHost
            Chain.TESTNET -> appSetting.testExplorerHost
            Chain.SIGNET -> appSetting.signetExplorerHost
            else -> appSetting.mainnetExplorerHost
        }
    }
} 