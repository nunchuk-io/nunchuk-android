package com.nunchuk.android.signer.components.ss.name

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface AddSoftwareSignerNameModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddSoftwareSignerNameViewModel::class)
    fun bindAddSoftwareSignerNameViewModel(viewModel: AddSoftwareSignerNameViewModel): ViewModel

}