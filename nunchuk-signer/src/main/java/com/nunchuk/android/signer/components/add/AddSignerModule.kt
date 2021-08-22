package com.nunchuk.android.signer.components.add

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface AddSignerModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddSignerViewModel::class)
    fun bindAddSignerViewModel(viewModel: AddSignerViewModel): ViewModel

}