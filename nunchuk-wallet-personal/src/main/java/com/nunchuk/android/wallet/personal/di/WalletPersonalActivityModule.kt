package com.nunchuk.android.wallet.personal.di

import com.nunchuk.android.wallet.personal.components.add.AddWalletActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface WalletPersonalActivityModule {

    @ContributesAndroidInjector
    fun addWalletActivity(): AddWalletActivity

}