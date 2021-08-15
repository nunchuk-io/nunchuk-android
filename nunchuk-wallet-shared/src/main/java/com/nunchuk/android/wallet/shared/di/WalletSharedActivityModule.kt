package com.nunchuk.android.wallet.shared.di

import com.nunchuk.android.wallet.shared.components.configure.ConfigureSharedWalletActivity
import com.nunchuk.android.wallet.shared.components.create.CreateSharedWalletActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface WalletSharedActivityModule {

    @ContributesAndroidInjector
    fun addSharedWalletActivity(): CreateSharedWalletActivity

    @ContributesAndroidInjector
    fun configureSharedWalletActivity(): ConfigureSharedWalletActivity

}