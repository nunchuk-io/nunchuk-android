package com.nunchuk.android.wallet.components.confirm

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletConfirmModule {

    @Binds
    @IntoMap
    @ViewModelKey(WalletConfirmViewModel::class)
    fun bindWalletConfirmViewModel(viewModel: WalletConfirmViewModel): ViewModel

}