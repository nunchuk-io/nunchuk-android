package com.nunchuk.android.transaction.receive

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ReceiveTransactionModule {

    @Binds
    @IntoMap
    @ViewModelKey(ReceiveTransactionViewModel::class)
    fun bindAddWalletViewModel(viewModel: ReceiveTransactionViewModel): ViewModel

}