package com.nunchuk.android.nativelib.di

import com.nunchuk.android.usecase.*
import dagger.Binds
import dagger.Module

@Module
internal abstract class NativeLibDomainModule {

    @Binds
    abstract fun bindCreateSignerUseCase(useCase: CreateSignerUseCaseImpl): CreateSignerUseCase

    @Binds
    abstract fun bindGetRemoteSignerUseCase(useCase: GetRemoteSignerUseCaseImpl): GetRemoteSignerUseCase

    @Binds
    abstract fun bindGetAppSettingsUseCase(useCase: GetAppSettingsUseCaseImpl): GetAppSettingsUseCase

    @Binds
    abstract fun bindInitNunchukUseCase(useCase: InitNunchukUseCaseImpl): InitNunchukUseCase

    @Binds
    abstract fun bindGetOrCreateRootDirUseCase(useCase: GetOrCreateRootDirUseCaseImpl): GetOrCreateRootDirUseCase

}