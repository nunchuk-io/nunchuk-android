package com.nunchuk.android.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.app.di.BootstrapInjectors
import com.nunchuk.android.core.base.ForegroundAppBackgroundListener
import com.nunchuk.android.core.matrix.MatrixInitializer
import com.nunchuk.android.core.util.AppEvenBus
import com.nunchuk.android.core.util.AppEvent
import com.nunchuk.android.util.FileHelper
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.matrix.android.sdk.api.Matrix
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

internal class NunchukApplication : Application(), HasAndroidInjector, Configuration.Provider {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var fileHelper: FileHelper

    @Inject
    lateinit var initializer: MatrixInitializer

    @Inject
    lateinit var matrix: Matrix

    @Inject
    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    private var foregroundAppBackgroundListener: ForegroundAppBackgroundListener? = null

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        BootstrapInjectors.inject(this)
        fileHelper.getOrCreateNunchukRootDir()
        initializer.initialize()
        registerAppForegroundListener()
    }

    private fun registerAppForegroundListener() {
        foregroundAppBackgroundListener = ForegroundAppBackgroundListener(
            onResumeAppCallback = { AppEvenBus.instance.publish(AppEvent.AppResumedEvent) }
        )
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


    override fun attachBaseContext(base: Context) {
        MultiDex.install(base)
        super.attachBaseContext(base)
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
