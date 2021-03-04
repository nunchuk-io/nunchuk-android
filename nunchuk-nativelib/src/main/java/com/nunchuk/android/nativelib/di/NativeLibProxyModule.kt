package com.nunchuk.android.nativelib.di

import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.CreateSignerUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
internal abstract class NativeLibDomainModule {

    @Binds
    abstract fun bindCreateSignerUseCase(useCase: CreateSignerUseCaseImpl): CreateSignerUseCase
}

@Module(includes = [
    NativeLibDomainModule::class
])
interface NativeLibProxyModule
