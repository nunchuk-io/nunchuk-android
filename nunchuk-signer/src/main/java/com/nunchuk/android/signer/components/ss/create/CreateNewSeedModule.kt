package com.nunchuk.android.signer.components.ss.create

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface CreateNewSeedModule {

    @Binds
    @IntoMap
    @ViewModelKey(CreateNewSeedViewModel::class)
    fun bindCreateNewSeedViewModel(viewModel: CreateNewSeedViewModel): ViewModel

}
