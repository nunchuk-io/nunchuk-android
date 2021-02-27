package com.nunchuk.android.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.multidex.MultiDex
import com.nunchuk.android.app.di.BootstrapInjectors
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import java.io.File
import javax.inject.Inject

class NunchukApplication : Application(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate() {
        super.onCreate()
        BootstrapInjectors.inject(this)
        createRootDir()
    }

    private fun createRootDir() {
        val dirPath = filesDir.absolutePath + File.separator.toString() + "nunchuk"
        val projDir = File(dirPath)
        if (!projDir.exists()) {
            projDir.mkdirs()
        }
    }

    override fun attachBaseContext(base: Context) {
        MultiDex.install(base);
        super.attachBaseContext(base)
    }

}