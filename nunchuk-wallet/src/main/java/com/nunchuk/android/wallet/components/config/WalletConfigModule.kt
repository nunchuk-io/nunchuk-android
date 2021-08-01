package com.nunchuk.android.wallet.components.config

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletConfigModule {

    @Binds
    @IntoMap
    @ViewModelKey(WalletConfigViewModel::class)
    fun bindWalletInfoViewModel(viewModel: WalletConfigViewModel): ViewModel

}