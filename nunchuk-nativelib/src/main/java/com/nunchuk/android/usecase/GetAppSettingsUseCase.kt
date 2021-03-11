package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.type.BackendType
import com.nunchuk.android.type.Chain
import io.reactivex.Single
import javax.inject.Inject

interface GetAppSettingsUseCase {
    fun execute(): Single<AppSettings>
}

internal class GetAppSettingsUseCaseImpl @Inject constructor(
    private val getOrCreateRootDirUseCase: GetOrCreateRootDirUseCase
) : GetAppSettingsUseCase {

    // TODO implement persistent layer to provide consistent AppSettings
    override fun execute() = getOrCreateRootDirUseCase.execute()
        .map {
            AppSettings(
                chain = Chain.TESTNET,
                hwiPath = "bin/hwi",
                testnetServers = listOf("testnet.nunchuk.io:50001"),
                backendType = BackendType.ELECTRUM,
                storagePath = it
            )
        }

}
