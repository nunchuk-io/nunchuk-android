package com.nunchuk.android.wallet.di

import dagger.Module

@Module(
    includes = [
        WalletActivityModule::class
    ]
)
interface WalletProxyModule