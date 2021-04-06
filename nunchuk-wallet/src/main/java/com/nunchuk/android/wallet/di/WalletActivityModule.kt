package com.nunchuk.android.wallet.di

import com.nunchuk.android.wallet.add.AddWalletActivity
import com.nunchuk.android.wallet.add.AddWalletModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface WalletActivityModule {

    @ContributesAndroidInjector(modules = [AddWalletModule::class])
    fun addWalletActivity(): AddWalletActivity
}