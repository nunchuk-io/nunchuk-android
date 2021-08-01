package com.nunchuk.android.wallet.components.details

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletDetailsModule {

    @Binds
    @IntoMap
    @ViewModelKey(WalletDetailsViewModel::class)
    fun bindWalletDetailsViewModel(viewModel: WalletDetailsViewModel): ViewModel

}