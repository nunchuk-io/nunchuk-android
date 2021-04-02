package com.nunchuk.android.app.di

import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.NunchukNavigatorImpl
import dagger.Binds
import dagger.Module

@Module
internal interface NunchukNavigatorModule {

    @Binds
    fun bindNunchukNavigator(nav: NunchukNavigatorImpl): NunchukNavigator

}