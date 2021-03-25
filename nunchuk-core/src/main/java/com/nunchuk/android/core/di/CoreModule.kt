package com.nunchuk.android.core.di

import com.nunchuk.android.network.HeaderProvider
import com.nunchuk.android.network.HeaderProviderImpl
import dagger.Binds
import dagger.Module

@Module
abstract class CoreModule {

    @Binds
    abstract fun bindHeaderProvider(provider: HeaderProviderImpl): HeaderProvider

}