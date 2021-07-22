package com.nunchuk.android.app.di

import android.content.Context
import com.nunchuk.android.app.NunchukApplication
import com.nunchuk.android.auth.di.AuthProxyModule
import com.nunchuk.android.contact.di.ContactProxyModule
import com.nunchuk.android.messages.di.MessagesProxyModule
import com.nunchuk.android.core.di.CoreProxyModule
import com.nunchuk.android.database.di.DatabaseProxyModule
import com.nunchuk.android.domain.di.NativeSDKProxyModule
import com.nunchuk.android.main.di.MainProxyModule
import com.nunchuk.android.network.di.NetworkProxyModule
import com.nunchuk.android.signer.di.SignerProxyModule
import com.nunchuk.android.transaction.di.TransactionProxyModule
import com.nunchuk.android.wallet.di.WalletProxyModule
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Module(includes = [AndroidInjectionModule::class])
internal interface BootstrapModule {
    @Binds
    fun application(app: NunchukApplication): Context
}

@Singleton
@Component(
    modules = [
        BootstrapModule::class,
        AppProxyModule::class,
        AuthProxyModule::class,
        CoreProxyModule::class,
        DatabaseProxyModule::class,
        MainProxyModule::class,
        SignerProxyModule::class,
        WalletProxyModule::class,
        TransactionProxyModule::class,
        ContactProxyModule::class,
        MessagesProxyModule::class,
        NetworkProxyModule::class,
        NativeSDKProxyModule::class
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
