package com.nunchuk.android.wallet.shared.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.wallet.shared.components.configure.ConfigureSharedWalletViewModel
import com.nunchuk.android.wallet.shared.components.create.CreateSharedWalletViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletSharedViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(CreateSharedWalletViewModel::class)
    fun bindCreateSharedWalletViewModel(viewModel: CreateSharedWalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfigureSharedWalletViewModel::class)
    fun bindConfigureSharedWalletViewModel(viewModel: ConfigureSharedWalletViewModel): ViewModel

}