package com.nunchuk.android.main.di

import com.nunchuk.android.main.components.account.AccountFragment
import com.nunchuk.android.main.components.account.AccountModule
import com.nunchuk.android.main.components.message.MessagesFragment
import com.nunchuk.android.main.components.message.MessagesModule
import com.nunchuk.android.main.components.wallet.WalletsFragment
import com.nunchuk.android.main.components.wallet.WalletsModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MainFragmentModule {

    @ContributesAndroidInjector(modules = [MessagesModule::class])
    fun messagesFragment(): MessagesFragment

    @ContributesAndroidInjector(modules = [WalletsModule::class])
    fun walletsFragment(): WalletsFragment

    @ContributesAndroidInjector(modules = [AccountModule::class])
    fun accountFragment(): AccountFragment

}