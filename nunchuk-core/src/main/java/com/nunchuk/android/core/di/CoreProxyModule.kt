package com.nunchuk.android.core.di

import dagger.Module

@Module(
    includes = [
        ViewModelModule::class,
        CoreModule::class
    ]
)
interface CoreProxyModule
