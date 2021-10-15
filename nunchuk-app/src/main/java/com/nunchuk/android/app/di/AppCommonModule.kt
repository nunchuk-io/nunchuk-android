package com.nunchuk.android.app.di

import com.nunchuk.android.app.provider.AppInfoProviderImpl
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.app.network.HeaderProviderImpl
import com.nunchuk.android.core.provider.AppInfoProvider
import dagger.Binds
import dagger.Module

@Module
internal interface AppCommonModule {

    @Binds
    fun bindAppInfoProvider(nav: AppInfoProviderImpl): AppInfoProvider

    @Binds
    fun bindHeaderProvider(provider: HeaderProviderImpl): HeaderProvider

}