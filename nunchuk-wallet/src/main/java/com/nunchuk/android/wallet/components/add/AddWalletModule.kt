package com.nunchuk.android.wallet.components.add

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface AddWalletModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddWalletViewModel::class)
    fun bindAddWalletViewModel(viewModel: AddWalletViewModel): ViewModel

}