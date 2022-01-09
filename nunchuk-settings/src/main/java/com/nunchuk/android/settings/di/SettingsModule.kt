package com.nunchuk.android.settings.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.settings.AccountFragment
import com.nunchuk.android.settings.AccountViewModel
import com.nunchuk.android.settings.network.NetworkSettingActivity
import com.nunchuk.android.settings.network.NetworkSettingViewModel
import com.nunchuk.android.settings.unit.DisplayUnitActivity
import com.nunchuk.android.settings.unit.DisplayUnitViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
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

}

@Module
internal interface SettingsFragmentModule {

    @ContributesAndroidInjector
    fun accountFragment(): AccountFragment

    @ContributesAndroidInjector
    fun networkSettingFragment(): NetworkSettingActivity

    @ContributesAndroidInjector
    fun displayUnitActivity(): DisplayUnitActivity

}

@Module(
    includes = [
        SettingsFragmentModule::class,
        SettingsViewModelModule::class
    ]
)
interface SettingsModule