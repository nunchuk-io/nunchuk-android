package com.nunchuk.android.wallet.components.configure

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ConfigureWalletModule {

    @Binds
    @IntoMap
    @ViewModelKey(ConfigureWalletViewModel::class)
    fun bindAssignSignerViewModel(viewModel: ConfigureWalletViewModel): ViewModel

}