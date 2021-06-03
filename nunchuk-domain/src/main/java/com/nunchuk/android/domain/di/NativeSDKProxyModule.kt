package com.nunchuk.android.domain.di

import dagger.Module

@Module(
    includes = [
        NativeCommonModule::class,
        SignerDomainModule::class,
        WalletDomainModule::class,
        TransactionDomainModule::class
    ]
)
interface NativeSDKProxyModule
