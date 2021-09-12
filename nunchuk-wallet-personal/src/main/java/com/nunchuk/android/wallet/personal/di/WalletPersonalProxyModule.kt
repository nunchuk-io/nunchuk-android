package com.nunchuk.android.wallet.personal.di

import dagger.Module

@Module(
    includes = [
        WalletPersonalActivityModule::class,
        WalletPersonalViewModelModule::class
    ]
)
interface WalletPersonalProxyModule
