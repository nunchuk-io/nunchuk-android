package com.nunchuk.android.wallet.shared.di

import com.nunchuk.android.wallet.shared.components.assign.AssignSignerSharedWalletActivity
import com.nunchuk.android.wallet.shared.components.config.SharedWalletConfigActivity
import com.nunchuk.android.wallet.shared.components.configure.ConfigureSharedWalletActivity
import com.nunchuk.android.wallet.shared.components.create.CreateSharedWalletActivity
import com.nunchuk.android.wallet.shared.components.review.ReviewSharedWalletActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface WalletSharedActivityModule {

    @ContributesAndroidInjector
    fun addSharedWalletActivity(): CreateSharedWalletActivity

    @ContributesAndroidInjector
    fun configureSharedWalletActivity(): ConfigureSharedWalletActivity

    @ContributesAndroidInjector
    fun reviewSharedWalletActivity(): ReviewSharedWalletActivity

    @ContributesAndroidInjector
    fun assignSignerSharedWalletActivity(): AssignSignerSharedWalletActivity

    @ContributesAndroidInjector
    fun sharedWalletConfigActivity(): SharedWalletConfigActivity

}