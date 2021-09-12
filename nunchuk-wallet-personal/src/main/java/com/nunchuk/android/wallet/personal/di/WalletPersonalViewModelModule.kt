package com.nunchuk.android.wallet.personal.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.wallet.personal.components.add.AddWalletViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletPersonalViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddWalletViewModel::class)
    fun bindAddWalletViewModel(viewModel: AddWalletViewModel): ViewModel

}