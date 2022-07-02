package com.nunchuk.android.core.di

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.AccountManagerImpl
import com.nunchuk.android.core.device.DeviceManagerImpl
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.core.loader.ImageLoaderImpl
import com.nunchuk.android.core.matrix.MatrixInterceptor
import com.nunchuk.android.core.matrix.MatrixInterceptorImpl
import com.nunchuk.android.utils.DeviceManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CoreModule {

    @Binds
    @Singleton
    fun bindAccountManager(manager: AccountManagerImpl): AccountManager

    @Binds
    @Singleton
    fun bindDeviceManager(manager: DeviceManagerImpl): DeviceManager

    @Binds
    @Singleton
    fun bindMatrixInterceptor(interceptor: MatrixInterceptorImpl): MatrixInterceptor

    @Binds
    @Singleton
    fun bindImageLoader(interceptor: ImageLoaderImpl): ImageLoader

}