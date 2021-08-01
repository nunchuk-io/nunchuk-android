package com.nunchuk.android.wallet.components.backup

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface BackupWalletModule {

    @Binds
    @IntoMap
    @ViewModelKey(BackupWalletViewModel::class)
    fun bindBackupWalletViewModel(viewModel: BackupWalletViewModel): ViewModel

}