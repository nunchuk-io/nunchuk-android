/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.share

import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.BLOCKCHAIN_STATUS
import com.nunchuk.android.core.util.toMatrixContent
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.ConnectionStatusExecutor
import com.nunchuk.android.model.ConnectionStatusHelper
import com.nunchuk.android.model.SendEventExecutor
import com.nunchuk.android.model.SendEventHelper
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.utils.DeviceManager
import com.nunchuk.android.utils.trySafe
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitNunchukUseCase @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val nativeSdk: NunchukNativeSdk,
    private val deviceManager: DeviceManager,
    private val sessionHolder: SessionHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<InitNunchukUseCase.Param, Boolean>(ioDispatcher) {
    private var lastParam : Param? = null
    private var lastSettings : AppSettings? = null

    override suspend fun execute(parameters: Param) : Boolean {
        val settings = getAppSettingUseCase(Unit).getOrThrow()
        if (parameters == lastParam && lastSettings == settings) return false
        lastSettings = settings
        lastParam = parameters
        Timber.d("InitNunchukUseCase: $settings")
        initNunchuk(
            appSettings = settings,
            passphrase = parameters.passphrase,
            accountId = parameters.accountId,
            deviceId = deviceManager.getDeviceId(),
            decoyPin = parameters.decoyPin
        )
        return true
    }

    private fun initNunchuk(
        appSettings: AppSettings,
        passphrase: String,
        accountId: String,
        deviceId: String,
        decoyPin: String
    ) {
        initReceiver()
        fileLog(message = "start nativeSdk initNunchuk")
        nativeSdk.initNunchuk(
            appSettings = appSettings,
            passphrase = passphrase,
            accountId = accountId,
            deviceId = deviceId,
            decoyPin = decoyPin
        )
        fileLog("end nativeSdk initNunchuk")
    }

    private fun initReceiver() {
        SendEventHelper.executor = object : SendEventExecutor {
            override fun execute(
                roomId: String,
                type: String,
                content: String,
                ignoreError: Boolean
            ): String {
                if (sessionHolder.hasActiveSession()) {
                    sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomId)?.apply {
                        trySafe {
                            sendService().sendEvent(
                                eventType = type,
                                content = content.toMatrixContent()
                            )
                        }
                    }
                }
                return ""
            }
        }

        ConnectionStatusHelper.executor = object : ConnectionStatusExecutor {
            override fun execute(connectionStatus: ConnectionStatus, percent: Int) {
                BLOCKCHAIN_STATUS = connectionStatus
            }
        }
    }

    data class Param(val passphrase: String = "", val accountId: String, val decoyPin: String = "")
}
