package com.nunchuk.android.app.di

import dagger.Module

@Module(
    includes = [
        AppActivityModule::class,
        NunchukNavigatorModule::class
    ]
)
interface AppProxyModule
