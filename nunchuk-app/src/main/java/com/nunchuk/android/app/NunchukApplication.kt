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

package com.nunchuk.android.app

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.ForegroundAppBackgroundListener
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.matrix.MatrixInitializerUseCase
import com.nunchuk.android.core.profile.CheckShowOnBoardFreshInstallUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.util.AppEvenBus
import com.nunchuk.android.core.util.AppEvent
import com.nunchuk.android.log.FileLogTree
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.util.FileHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.Matrix
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
internal class NunchukApplication : MultiDexApplication(), Configuration.Provider {

    @Inject
    lateinit var fileHelper: FileHelper

    @Inject
    lateinit var matrix: Matrix

    @Inject
    lateinit var matrixInitializerUseCase: MatrixInitializerUseCase

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var initNunchukUseCase: InitNunchukUseCase

    @Inject
    lateinit var getUserProfileUseCase: GetUserProfileUseCase

    @Inject
    lateinit var checkShowOnBoardFreshInstallUseCase: CheckShowOnBoardFreshInstallUseCase

    @Inject
    lateinit var applicationScope: CoroutineScope

    private val foregroundAppBackgroundListener = ForegroundAppBackgroundListener(
        onResumeAppCallback = {
            AppEvenBus.instance.publish(AppEvent.AppResumedEvent)
            if (accountManager.getAccount().token.isNotEmpty()) {
                applicationScope.launch {
                    getUserProfileUseCase(Unit)
                }
            }
        }
    )

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(FileLogTree(this))
        }
        runBlocking {
            checkShowOnBoardFreshInstallUseCase(Unit)
            matrixInitializerUseCase(Unit)
            val account = accountManager.getAccount()
            val accountId = if (account.loginType == SignInMode.PRIMARY_KEY.value) {
                account.username
            } else {
                account.email
            }
            initNunchukUseCase(InitNunchukUseCase.Param(accountId = accountId))
        }
        fileHelper.getOrCreateNunchukRootDir()
        registerActivityLifecycleCallbacks(ActivityManager)
        registerAppForegroundListener()
    }

    private fun registerAppForegroundListener() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(NcToastManager)
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(foregroundAppBackgroundListener)
    }

    override fun getWorkManagerConfiguration() = Configuration.Builder()
        .setWorkerFactory(matrix.getWorkerFactory())
        .setExecutor(Executors.newCachedThreadPool())
        .build()
}
