package com.nunchuk.android.core.di

import dagger.Module

@Module(
    includes = [
        ViewModelModule::class,
        CoreModule::class,
        MatrixProxyModule::class
    ]
)
interface CoreProxyModule
