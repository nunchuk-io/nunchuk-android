package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface InitNunchukUseCase {
    fun execute(
        passphrase: String,
        accountId: String
    ): Flow<Unit>
}

internal class InitNunchukUseCaseImpl @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingsUseCase,
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), InitNunchukUseCase {

    override fun execute(
        passphrase: String,
        accountId: String
    ) = getAppSettingUseCase.execute().flatMapConcat {
        initNunchuk(
            appSettings = it,
            accountId = accountId
        )
    }

    private fun initNunchuk(
        appSettings: AppSettings,
        accountId: String
    ) = flow {
        emit(nativeSdk.initNunchuk(appSettings, "", accountId))
    }.flowOn(Dispatchers.IO)

}