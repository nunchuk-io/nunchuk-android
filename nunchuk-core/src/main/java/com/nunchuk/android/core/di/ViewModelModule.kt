package com.nunchuk.android.core.di

import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.arch.vm.ViewModelFactory
import dagger.Binds
import dagger.Module

@Module
interface ViewModelModule {

    @Binds
    fun bindViewModelFactory(factory: ViewModelFactory): NunchukFactory

}