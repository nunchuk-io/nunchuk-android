package com.nunchuk.android.wallet.di

import com.nunchuk.android.wallet.shared.di.WalletSharedModule
import dagger.Module

@Module(
    includes = [
        WalletActivityModule::class,
        WalletSharedModule::class
    ]
)
interface WalletProxyModule