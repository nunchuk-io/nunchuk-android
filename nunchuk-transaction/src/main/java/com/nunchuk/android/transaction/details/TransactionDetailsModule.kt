package com.nunchuk.android.transaction.details

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface TransactionDetailsModule {

    @Binds
    @IntoMap
    @ViewModelKey(TransactionDetailsViewModel::class)
    fun bindTransactionDetailsViewModel(viewModel: TransactionDetailsViewModel): ViewModel

}