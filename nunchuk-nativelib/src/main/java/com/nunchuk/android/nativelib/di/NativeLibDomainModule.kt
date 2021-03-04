package com.nunchuk.android.nativelib.di

import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.CreateSignerUseCaseImpl
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
internal abstract class NativeLibDomainModule {

    @Binds
    abstract fun bindCreateSignerUseCase(useCase: CreateSignerUseCaseImpl): CreateSignerUseCase

    @Binds
    abstract fun bindGetRemoteSignerUseCase(useCase: GetRemoteSignerUseCaseImpl): GetRemoteSignerUseCase

}