package com.nunchuk.android.app.di

import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.app.nav.NunchukNavigatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NunchukNavigatorModule {

    @Binds
    fun bindNunchukNavigator(nav: NunchukNavigatorImpl): NunchukNavigator

}