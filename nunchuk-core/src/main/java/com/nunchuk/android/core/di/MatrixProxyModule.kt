package com.nunchuk.android.core.di

import dagger.Module

@Module(
    includes = [
        MatrixNetworkModule::class,
        MatrixDataModule::class,
        MatrixDomainModule::class
    ]
)
interface MatrixProxyModule
