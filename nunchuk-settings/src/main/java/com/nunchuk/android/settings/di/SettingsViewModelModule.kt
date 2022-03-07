package com.nunchuk.android.settings.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.settings.AccountSettingViewModel
import com.nunchuk.android.settings.AccountViewModel
import com.nunchuk.android.settings.DeleteAccountViewModel
import com.nunchuk.android.settings.network.NetworkSettingViewModel
import com.nunchuk.android.settings.unit.DisplayUnitViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface SettingsViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(AccountViewModel::class)
    fun bindAccountViewModel(viewModel: AccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkSettingViewModel::class)
    fun bindNetworkSettingViewModel(viewModel: NetworkSettingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DisplayUnitViewModel::class)
    fun bindDisplayUnitViewModel(viewModel: DisplayUnitViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AccountSettingViewModel::class)
    fun bindAccountSettingViewModel(viewModel: AccountSettingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DeleteAccountViewModel::class)
    fun bindDeleteAccountViewModel(viewModel: DeleteAccountViewModel): ViewModel

}