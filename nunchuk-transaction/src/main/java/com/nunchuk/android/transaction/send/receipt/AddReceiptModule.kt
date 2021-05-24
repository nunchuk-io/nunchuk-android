package com.nunchuk.android.transaction.send.receipt

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface AddReceiptModule {

    @Binds
    @IntoMap
    @ViewModelKey(TransactionConfirmViewModel::class)
    fun bindAddReceiptViewModel(viewModel: TransactionConfirmViewModel): ViewModel

}