package com.nunchuk.android.app.di

import dagger.Module

@Module(
    includes = [
        AppActivityModule::class
    ]
)
interface AppProxyModule
