package com.nunchuk.android.wallet.personal.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface AddRecoverWalletViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(RecoverWalletViewModel::class)
    fun bindRecoverWalletViewModel(viewModel: RecoverWalletViewModel): ViewModel
}