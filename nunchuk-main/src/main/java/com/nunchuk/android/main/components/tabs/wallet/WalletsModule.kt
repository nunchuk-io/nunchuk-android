package com.nunchuk.android.main.components.tabs.wallet

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface WalletsModule {

    @Binds
    @IntoMap
    @ViewModelKey(WalletsViewModel::class)
    fun bindWalletsViewModel(viewModel: WalletsViewModel): ViewModel

}