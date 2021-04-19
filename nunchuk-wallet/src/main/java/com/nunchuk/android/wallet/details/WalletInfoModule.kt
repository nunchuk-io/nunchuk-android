package com.nunchuk.android.wallet.details

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletInfoModule {

    @Binds
    @IntoMap
    @ViewModelKey(WalletInfoViewModel::class)
    fun bindWalletInfoViewModel(viewModel: WalletInfoViewModel): ViewModel

}