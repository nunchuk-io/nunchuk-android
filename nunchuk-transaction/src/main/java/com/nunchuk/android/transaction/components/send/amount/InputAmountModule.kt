package com.nunchuk.android.transaction.components.send.amount

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface InputAmountModule {

    @Binds
    @IntoMap
    @ViewModelKey(InputAmountViewModel::class)
    fun bindInputAmountViewModel(viewModel: InputAmountViewModel): ViewModel

}