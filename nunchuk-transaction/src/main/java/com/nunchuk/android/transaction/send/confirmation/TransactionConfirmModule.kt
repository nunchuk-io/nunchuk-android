package com.nunchuk.android.transaction.send.confirmation

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface TransactionConfirmModule {

    @Binds
    @IntoMap
    @ViewModelKey(TransactionConfirmViewModel::class)
    fun bindTransactionConfirmViewModel(viewModel: TransactionConfirmViewModel): ViewModel

}