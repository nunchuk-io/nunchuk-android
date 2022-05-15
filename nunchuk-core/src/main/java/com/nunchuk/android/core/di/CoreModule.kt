package com.nunchuk.android.core.di

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.AccountManagerImpl
import com.nunchuk.android.core.device.DeviceManagerImpl
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.core.loader.ImageLoaderImpl
import com.nunchuk.android.core.matrix.MatrixInterceptor
import com.nunchuk.android.core.matrix.MatrixInterceptorImpl
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.utils.DeviceManager
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface CoreModule {

    @Binds
    fun bindAccountManager(manager: AccountManagerImpl): AccountManager

    @Binds
    fun bindDeviceManager(manager: DeviceManagerImpl): DeviceManager

    @Binds
    fun bindMatrixInterceptor(interceptor: MatrixInterceptorImpl): MatrixInterceptor

    @Binds
    fun bindImageLoader(interceptor: ImageLoaderImpl): ImageLoader

    companion object {
        @Provides
        fun provideMatrixSession() = SessionHolder.activeSession!!
    }

}