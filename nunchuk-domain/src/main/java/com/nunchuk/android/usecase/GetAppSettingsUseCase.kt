package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.type.BackendType
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetAppSettingsUseCase {
    fun execute(): Flow<AppSettings>
}

internal class GetAppSettingsUseCaseImpl @Inject constructor(
    private val getOrCreateRootDirUseCase: GetOrCreateRootDirUseCase
) : GetAppSettingsUseCase {

    override fun execute() = flow {
        emit(
            AppSettings(
                chain = Chain.TESTNET,
                hwiPath = "bin/hwi",
                testnetServers = listOf("testnet.nunchuk.io:50001"),
                backendType = BackendType.ELECTRUM,
                storagePath = getOrCreateRootDirUseCase.execute()
            )
        )
    }

}
