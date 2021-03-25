package com.nunchuk.android.auth.di

import dagger.Module

@Module(
    includes = [
        AuthActivityModule::class,
        AuthDomainModule::class,
        AuthDataModule::class,
        AuthNetworkModule::class,
        AuthCommonModule::class
    ]
)
interface AuthProxyModule
