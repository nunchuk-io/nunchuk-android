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

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.persistence.InternalEncryptKeyStore
import com.nunchuk.android.core.persistence.PreferenceKeyHasher
import com.nunchuk.android.core.util.toMatrixContent
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.SendEventExecutor
import com.nunchuk.android.model.SendEventHelper
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.utils.DeviceManager
import com.nunchuk.android.utils.trySafe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class InitNunchukUseCase @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val nativeSdk: NunchukNativeSdk,
    private val deviceManager: DeviceManager,
    private val sessionHolder: SessionHolder,
    private val enableGroupWalletUseCase: EnableGroupWalletUseCase,
    private val startConsumeGroupWalletEventUseCase: StartConsumeGroupWalletEventUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val applicationScope: CoroutineScope,
    private val accountManager: AccountManager,
    private val internalEncryptKeyStore: InternalEncryptKeyStore,
    private val preferenceKeyHasher: PreferenceKeyHasher,
) : UseCase<InitNunchukUseCase.Param, Boolean>(ioDispatcher) {
    private var lastParam: Param? = null
    private var lastSettings: AppSettings? = null
    private var consumeJob: Job? = null
    private var lastDecoyPin: String = ""

    override suspend fun execute(parameters: Param): Boolean {
        val settings = getAppSettingUseCase(Unit).getOrThrow()
        val decoyPin = accountManager.getLastDecoyPin()
        if (decoyPin.isNotEmpty()) {
            accountManager.storeAccount(
                AccountInfo(
                    decoyPin = decoyPin,
                    loginType = SignInMode.GUEST_MODE.value
                )
            )
        }
        if (parameters == lastParam && lastSettings == settings && lastDecoyPin == decoyPin) return false
        Timber.d("InitNunchukUseCase: $settings")
        consumeJob?.cancel()
        initNunchuk(
            appSettings = settings,
            passphrase = parameters.passphrase,
            accountId = parameters.accountId,
            deviceId = deviceManager.getDeviceId(),
            decoyPin = decoyPin
        )
        // Only after a successful init, so a failed one (e.g. key unreadable) is retried next time.
        lastSettings = settings
        lastParam = parameters
        lastDecoyPin = decoyPin
        fileLog(message = "start nativeSdk enableGroupWalletUseCase")
        Timber.d("Thread: ${Thread.currentThread().name}")
        consumeJob = applicationScope.launch {
            initGroupWallet()
        }
        fileLog(message = "end nativeSdk enableGroupWalletUseCase")
        return true
    }

    private suspend fun initGroupWallet() {
        var retryCount = 0
        val maxRetries = 3
        val retryDelay = 500L

        while (coroutineContext.isActive) {
            val result = runCatching {
                enableGroupWalletUseCase(Unit)
                nativeSdk.registerGlobalListener()
                startConsumeGroupWalletEventUseCase(Unit)
            }

            if (result.isSuccess) {
                Timber.d("initGroupWallet success")
                break
            } else {
                if (retryCount < maxRetries) {
                    retryCount++
                    Timber.e(result.exceptionOrNull(), "Attempt $retryCount failed")
                    delay(retryDelay)
                } else {
                    Timber.e(result.exceptionOrNull(), "All $maxRetries retry attempts failed")
                    break
                }
            }
        }
    }

    suspend fun retryInitGroupWallet(): Result<Unit> {
        return runCatching {
            enableGroupWalletUseCase(Unit)
            nativeSdk.registerGlobalListener()
            startConsumeGroupWalletEventUseCase(Unit)
        }
    }

    private fun initNunchuk(
        appSettings: AppSettings,
        passphrase: String,
        accountId: String,
        deviceId: String,
        decoyPin: String
    ) {
        initReceiver()
        // Throws if the key exists but cannot be read: never open the DB without it, or new
        // mnemonics would be written in the clear and encrypted ones would fail to load.
        val encryptKey = internalEncryptKeyStore.getOrCreateKey()
        val identity = storageIdentity(appSettings.chain, accountId, decoyPin)
        val recovery = internalEncryptKeyStore.isRecoveryPending(identity)
        fileLog(message = "start nativeSdk initNunchuk")
        try {
            nativeSdk.initNunchuk(
                appSettings = appSettings,
                passphrase = passphrase,
                accountId = accountId,
                deviceId = deviceId,
                decoyPin = decoyPin,
                baseApiUrl = if (appSettings.chain == Chain.MAIN) "https://api.nunchuk.io" else "https://api-testnet.nunchuk.io",
                internalEncryptKey = encryptKey,
                recoveryMode = recovery,
            )
        } finally {
            encryptKey.fill(0)
        }
        if (recovery) sweepUnreadableSecrets(identity)
        fileLog("end nativeSdk initNunchuk")
    }

    /**
     * The previous internal encrypt key is gone, so this account's on-device mnemonics are
     * unreadable. Listing the master signers reads every mnemonic / master xprv, and recovery mode
     * erases each one it cannot decrypt (those signers become FOREIGN_SOFTWARE until the user
     * recovers them from their backup). Recovery is switched off afterwards so a later bug that
     * passes a wrong key fails loudly instead of erasing more.
     */
    private fun sweepUnreadableSecrets(identity: String) {
        runCatching { nativeSdk.getMasterSigners() }
            .onSuccess { internalEncryptKeyStore.markRecovered(identity) }
            .onFailure { Timber.e(it, "Internal encrypt key recovery sweep failed") }
        nativeSdk.recoveryMode(false)
    }

    /**
     * One set of libnunchuk signer DBs per chain and account / decoy pin, each swept on its own.
     * Hashed so neither the email nor the pin lands on disk.
     */
    private fun storageIdentity(chain: Chain, accountId: String, decoyPin: String): String =
        preferenceKeyHasher.derive(
            if (decoyPin.isNotEmpty()) "$chain:decoy:$decoyPin" else "$chain:account:$accountId"
        )

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
    }

    data class Param(val passphrase: String = "", val accountId: String)
}
