package com.nunchuk.android.transaction.send.receipt

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface AddReceiptModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddReceiptViewModel::class)
    fun bindAddReceiptViewModel(viewModel: AddReceiptViewModel): ViewModel

}