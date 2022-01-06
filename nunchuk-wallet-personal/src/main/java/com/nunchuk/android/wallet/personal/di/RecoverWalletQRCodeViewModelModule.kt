package com.nunchuk.android.wallet.personal.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletQrCodeViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface RecoverWalletQRCodeViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(RecoverWalletQrCodeViewModel::class)
    fun bindRecoverWalletQrCodeViewModel(viewModel: RecoverWalletQrCodeViewModel): ViewModel
}