package com.nunchuk.android.usecase

import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.SendEventExecutor
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface InitNunchukUseCase {
    fun execute(
        passphrase: String,
        accountId: String,
        executor: SendEventExecutor
    ): Flow<Unit>
}

internal class InitNunchukUseCaseImpl @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingsUseCase,
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), InitNunchukUseCase {

    override fun execute(passphrase: String, accountId: String, executor: SendEventExecutor): Flow<Unit> {
        return getAppSettingUseCase.execute().flatMapConcat { initNunchuk(it, passphrase, accountId, executor = executor) }
    }

    private fun initNunchuk(
        appSettings: AppSettings,
        passphrase: String,
        accountId: String,
        executor: SendEventExecutor
    ) = flow {
        emit(nativeSdk.initNunchuk(appSettings, passphrase, accountId, executor))
    }

}