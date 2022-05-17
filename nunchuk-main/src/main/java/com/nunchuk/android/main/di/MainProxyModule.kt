package com.nunchuk.android.main.di

import dagger.Module

@Module(
    includes = [
        MainActivityModule::class,
        MainViewModelModule::class,
        SyncStateModule::class
    ]
)
interface MainProxyModule
