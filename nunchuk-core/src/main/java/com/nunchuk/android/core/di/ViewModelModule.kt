package com.nunchuk.android.core.di

import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.share.InitNunchukUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ViewModelModule {

    @Binds
    fun bindInitNunchukUseCase(useCase: InitNunchukUseCaseImpl): InitNunchukUseCase

}