package com.nunchuk.android.transaction.receive.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.transaction.receive.address.unused.UnusedAddressFragment
import com.nunchuk.android.transaction.receive.address.unused.UnusedAddressViewModel
import com.nunchuk.android.transaction.receive.address.used.UsedAddressFragment
import com.nunchuk.android.transaction.receive.address.used.UsedAddressViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module(includes = [AddressViewModelModule::class])
internal abstract class AddressFragmentModule {

    @ContributesAndroidInjector
    abstract fun unusedAddressFragment(): UnusedAddressFragment

    @ContributesAndroidInjector
    abstract fun usedAddressFragment(): UsedAddressFragment
}

@Module
internal abstract class AddressViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(UnusedAddressViewModel::class)
    abstract fun bindUnusedAddressViewModel(viewModel: UnusedAddressViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UsedAddressViewModel::class)
    abstract fun bindUsedAddressViewModel(viewModel: UsedAddressViewModel): ViewModel

}