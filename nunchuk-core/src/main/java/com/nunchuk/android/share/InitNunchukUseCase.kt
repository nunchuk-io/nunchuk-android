package com.nunchuk.android.share

import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.InitAppSettingsUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.toMatrixContent
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.SendEventExecutor
import com.nunchuk.android.model.SendEventHelper
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
    private val iniAppSettingsUseCase: InitAppSettingsUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val nativeSdk: NunchukNativeSdk
) : InitNunchukUseCase {

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
        initReceiver()
        emit(nativeSdk.run {
            initNunchuk(appSettings, "", accountId)
            enableGenerateReceiveEvent()
        })
    }.flowOn(Dispatchers.IO)

    private fun initReceiver() {
        SendEventHelper.executor = object : SendEventExecutor {
            override fun execute(roomId: String, type: String, content: String): String {
                if (SessionHolder.hasActiveSession()) {
                    SessionHolder.activeSession?.getRoom(roomId)?.run {
                        sendEvent(type, content.toMatrixContent())
                    }
                }
                return ""
            }
        }
    }
}
