package com.nunchuk.android.domain.di

import dagger.Module

@Module(
    includes = [
        NativeCommonModule::class,
        SignerDomainModule::class,
        WalletDomainModule::class,
        RoomWalletDomainModule::class,
        TransactionDomainModule::class,
        RoomTransactionDomainModule::class
    ]
)
interface NativeSDKProxyModule
