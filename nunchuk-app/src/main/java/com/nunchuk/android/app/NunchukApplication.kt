/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.core.base.ForegroundAppBackgroundListener
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.AppEvenBus
import com.nunchuk.android.core.util.AppEvent
import com.nunchuk.android.log.FileLogTree
import com.nunchuk.android.util.FileHelper
import dagger.hilt.android.HiltAndroidApp
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

    private val foregroundAppBackgroundListener = ForegroundAppBackgroundListener(
        onResumeAppCallback = { AppEvenBus.instance.publish(AppEvent.AppResumedEvent) }
    )

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(FileLogTree(this))
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
