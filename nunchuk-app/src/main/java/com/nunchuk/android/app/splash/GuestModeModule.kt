package com.nunchuk.android.app.splash

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal abstract class GuestModeModule {

    @Binds
    @IntoMap
    @ViewModelKey(GuestModeViewModel::class)
    abstract fun bindGuestModeViewModel(viewModel: GuestModeViewModel): ViewModel

}