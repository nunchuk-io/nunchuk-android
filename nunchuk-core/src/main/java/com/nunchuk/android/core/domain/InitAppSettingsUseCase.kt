package com.nunchuk.android.core.domain

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.type.BackendType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.GetOrCreateRootDirUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import javax.inject.Inject

interface InitAppSettingsUseCase {
    fun execute(): Flow<AppSettings>
}

internal class InitAppSettingsUseCaseImpl @Inject constructor(
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val getOrCreateRootDirUseCase: GetOrCreateRootDirUseCase
) : InitAppSettingsUseCase {

    override fun execute() = getOrCreateRootDirUseCase.execute().flatMapConcat { path ->
        updateAppSettingUseCase.execute(
            AppSettings(
                chain = Chain.TESTNET,
                hwiPath = "bin/hwi",
                testnetServers = listOf("testnet.nunchuk.io:50001"),
                mainnetServers = listOf("mainnet.nunchuk.io:51001"),
                backendType = BackendType.ELECTRUM,
                storagePath = path
            )
        )
    }
}
