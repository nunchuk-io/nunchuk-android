package com.nunchuk.android.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.app.di.BootstrapInjectors
import com.nunchuk.android.core.matrix.MatrixInitializer
import com.nunchuk.android.core.matrix.RoomDisplayNameFallbackProviderImpl
import com.nunchuk.android.util.FileHelper
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.matrix.android.sdk.api.MatrixConfiguration
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

internal class NunchukApplication : Application(), HasAndroidInjector, MatrixConfiguration.Provider, Configuration.Provider {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var fileHelper: FileHelper

    @Inject
    lateinit var matrix: MatrixInitializer

    @Inject
    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        BootstrapInjectors.inject(this)
        fileHelper.getOrCreateNunchukRootDir()
        matrix.initialize()
    }

    override fun attachBaseContext(base: Context) {
        MultiDex.install(base)
        super.attachBaseContext(base)
    }

    override fun providesMatrixConfiguration() = MatrixConfiguration(
        roomDisplayNameFallbackProvider = RoomDisplayNameFallbackProviderImpl()
    )

    override fun getWorkManagerConfiguration() = Configuration.Builder()
        .setExecutor(Executors.newCachedThreadPool())
        .build()

    override fun onTerminate() {
        super.onTerminate()
        matrix.terminate()
    }
}
