package com.nunchuk.android.core.di

import dagger.Module

@Module(
    includes = [
        ViewModelModule::class,
        CoreModule::class,
        MatrixProxyModule::class,
        UserProfileProxyModule::class,
        DataModule::class,
        DomainModule::class,
        NetworkModule::class
    ]
)
interface CoreProxyModule
