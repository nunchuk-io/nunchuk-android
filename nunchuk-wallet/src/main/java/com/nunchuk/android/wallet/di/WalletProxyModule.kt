package com.nunchuk.android.wallet.di

import com.nunchuk.android.wallet.personal.di.WalletPersonalProxyModule
import com.nunchuk.android.wallet.shared.di.WalletSharedProxyModule
import dagger.Module

@Module(
    includes = [
        WalletActivityModule::class,
        WalletSharedProxyModule::class,
        WalletPersonalProxyModule::class
    ]
)
interface WalletProxyModule