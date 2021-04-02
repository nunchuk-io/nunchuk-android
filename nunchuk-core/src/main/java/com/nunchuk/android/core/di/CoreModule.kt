package com.nunchuk.android.core.di

import com.nunchuk.android.core.account.AccountManagerImpl
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.network.HeaderProvider
import com.nunchuk.android.network.HeaderProviderImpl
import dagger.Binds
import dagger.Module

@Module
interface CoreModule {

    @Binds
    fun bindHeaderProvider(provider: HeaderProviderImpl): HeaderProvider

    @Binds
    fun bindAccountManager(accountManager: AccountManagerImpl): AccountManager

}