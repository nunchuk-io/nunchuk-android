package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.Result
import com.nunchuk.android.type.BackendType
import com.nunchuk.android.type.Chain
import javax.inject.Inject

interface GetAppSettingsUseCase {
    suspend fun execute(): Result<AppSettings>
}

internal class GetAppSettingsUseCaseImpl @Inject constructor(
    private val getOrCreateRootDirUseCase: GetOrCreateRootDirUseCase
) : BaseUseCase(), GetAppSettingsUseCase {

    // TODO implement persistent layer to provide consistent AppSettings
    override suspend fun execute() = exe {
        AppSettings(
            chain = Chain.TESTNET,
            hwiPath = "bin/hwi",
            testnetServers = listOf("testnet.nunchuk.io:50001"),
            backendType = BackendType.ELECTRUM,
            storagePath = getOrCreateRootDirUseCase.execute()
        )
    }

}
