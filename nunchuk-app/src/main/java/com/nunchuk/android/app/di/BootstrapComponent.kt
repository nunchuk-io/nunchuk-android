package com.nunchuk.android.app.di

import android.content.Context
import com.nunchuk.android.app.NunchukApplication
import com.nunchuk.android.nativelib.di.NativeLibProxyModule
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import com.nunchuk.android.auth.di.AuthProxyModule
import com.nunchuk.android.core.di.CoreProxyModule
import com.nunchuk.android.database.di.DatabaseProxyModule
import com.nunchuk.android.network.di.NetworkProxyModule
import javax.inject.Singleton

@Module(includes = [AndroidInjectionModule::class])
internal abstract class BootstrapModule {
    @Binds
    abstract fun application(app: NunchukApplication): Context
}

@Singleton
@Component(
    modules = [
        BootstrapModule::class,
        AppProxyModule::class,
        AuthProxyModule::class,
        CoreProxyModule::class,
        DatabaseProxyModule::class,
        NetworkProxyModule::class,
        NativeLibProxyModule::class
    ]
)
internal interface BootstrapComponent : AndroidInjector<NunchukApplication> {

    @Component.Factory
    abstract class Factory : AndroidInjector.Factory<NunchukApplication>

}

internal object BootstrapInjectors {

    @JvmStatic
    fun inject(app: NunchukApplication) {
        DaggerBootstrapComponent.factory().create(app).inject(app)
    }

}
