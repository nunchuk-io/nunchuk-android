package com.nunchuk.android.core.di

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.AccountManagerImpl
import com.nunchuk.android.core.matrix.MatrixInterceptor
import com.nunchuk.android.core.matrix.MatrixInterceptorImpl
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.core.network.HeaderProviderImpl
import dagger.Binds
import dagger.Module

@Module
internal interface CoreModule {

    @Binds
    fun bindHeaderProvider(provider: HeaderProviderImpl): HeaderProvider

    @Binds
    fun bindAccountManager(manager: AccountManagerImpl): AccountManager

    @Binds
    fun bindMatrixInterceptor(interceptor: MatrixInterceptorImpl): MatrixInterceptor

}