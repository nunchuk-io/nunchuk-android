package com.nunchuk.android.app.di

import android.content.Context
import com.nunchuk.android.app.NunchukApplication
import com.nunchuk.android.nativelib.di.NativeLibProxyModule
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Module(includes = [AndroidInjectionModule::class])
abstract class BootstrapModule {
    @Binds
    abstract fun application(app: NunchukApplication): Context
}

@Singleton
@Component(
        modules = [
            BootstrapModule::class,
            AppProxyModule::class,
            NativeLibProxyModule::class
        ]
)
interface BootstrapComponent : AndroidInjector<NunchukApplication> {

    @Component.Factory
    abstract class Factory : AndroidInjector.Factory<NunchukApplication>

}

object BootstrapInjectors {

    @JvmStatic
    fun inject(app: NunchukApplication) {
        DaggerBootstrapComponent.factory().create(app).inject(app)
    }

}
