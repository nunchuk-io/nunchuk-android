package com.nunchuk.android.wallet.shared.di

import dagger.Module

@Module(
    includes = [
        WalletSharedActivityModule::class,
        WalletSharedViewModelModule::class
    ]
)
interface WalletSharedModule
