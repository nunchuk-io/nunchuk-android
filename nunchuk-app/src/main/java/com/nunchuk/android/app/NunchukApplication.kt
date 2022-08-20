package com.nunchuk.android.app

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.base.ForegroundAppBackgroundListener
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.matrix.MatrixInitializer
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
    lateinit var initializer: MatrixInitializer

    @Inject
    lateinit var matrix: Matrix

    private var foregroundAppBackgroundListener: ForegroundAppBackgroundListener? = null

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(FileLogTree(this))
        }
        fileHelper.getOrCreateNunchukRootDir()
        initializer.initialize()
        registerActivityLifecycleCallbacks(ActivityManager)
        registerAppForegroundListener()
    }

    private fun registerAppForegroundListener() {
        foregroundAppBackgroundListener = ForegroundAppBackgroundListener(
            onResumeAppCallback = { AppEvenBus.instance.publish(AppEvent.AppResumedEvent) }
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(NcToastManager)
        foregroundAppBackgroundListener?.let {
            ProcessLifecycleOwner.get()
                .lifecycle
                .addObserver(it)
        }
    }

    private fun removeAppForegroundListener() {
        foregroundAppBackgroundListener?.let {
            ProcessLifecycleOwner.get()
                .lifecycle
                .removeObserver(it)
        }
        foregroundAppBackgroundListener = null
    }

    override fun getWorkManagerConfiguration() = Configuration.Builder()
        .setWorkerFactory(matrix.getWorkerFactory())
        .setExecutor(Executors.newCachedThreadPool())
        .build()

    override fun onTerminate() {
        super.onTerminate()
        initializer.terminate()
        removeAppForegroundListener()
    }
}
