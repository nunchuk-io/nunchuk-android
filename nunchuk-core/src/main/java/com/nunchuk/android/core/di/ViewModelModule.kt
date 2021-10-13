package com.nunchuk.android.core.di

import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.share.InitNunchukUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
internal interface ViewModelModule {

    @Binds
    fun bindViewModelFactory(factory: ViewModelFactory): NunchukFactory

    @Binds
    fun bindInitNunchukUseCase(useCase: InitNunchukUseCaseImpl): InitNunchukUseCase

}