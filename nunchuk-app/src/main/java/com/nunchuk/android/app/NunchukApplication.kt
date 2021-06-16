package com.nunchuk.android.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.nunchuk.android.app.di.BootstrapInjectors
import com.nunchuk.android.core.matrix.MatrixInitializer
import com.nunchuk.android.util.FileHelper
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

internal class NunchukApplication : Application(), HasAndroidInjector {

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

        BootstrapInjectors.inject(this)
        fileHelper.getOrCreateNunchukRootDir()
        matrix.initialize()
    }

    override fun attachBaseContext(base: Context) {
        MultiDex.install(base)
        super.attachBaseContext(base)
    }

}
