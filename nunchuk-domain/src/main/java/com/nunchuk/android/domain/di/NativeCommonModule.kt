package com.nunchuk.android.domain.di

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetChainTipUseCaseImpl
import com.nunchuk.android.usecase.GetDevicesUseCase
import com.nunchuk.android.usecase.GetDevicesUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal interface NativeCommonModule {

    @Binds
    fun bindGetChainTipUseCase(useCase: GetChainTipUseCaseImpl): GetChainTipUseCase

    @Binds
    fun bindGetDeviceUseCase(useCase: GetDevicesUseCaseImpl): GetDevicesUseCase

    companion object {

        @Singleton
        @Provides
        fun provideNativeSdk() = NativeSdkProvider.instance.nativeSdk

    }
}

internal class NativeSdkProvider {
    val nativeSdk = NunchukNativeSdk()

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = NativeSdkProvider()
    }
}


