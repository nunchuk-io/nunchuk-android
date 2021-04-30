package com.nunchuk.android.signer.ss.confirm

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ConfirmSeedModule {

    @Binds
    @IntoMap
    @ViewModelKey(ConfirmSeedViewModel::class)
    fun bindConfirmSeedViewModel(viewModel: ConfirmSeedViewModel): ViewModel

}