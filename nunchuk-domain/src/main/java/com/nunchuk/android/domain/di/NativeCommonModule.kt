package com.nunchuk.android.domain.di

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.CreateShareFileUseCaseImpl
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetChainTipUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NativeCommonModule {

    @Binds
    fun bindGetChainTipUseCase(useCase: GetChainTipUseCaseImpl): GetChainTipUseCase

    @Binds
    fun bindCreateShareFileUseCase(useCase: CreateShareFileUseCaseImpl): CreateShareFileUseCase

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


