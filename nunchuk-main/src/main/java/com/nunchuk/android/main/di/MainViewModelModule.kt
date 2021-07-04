package com.nunchuk.android.main.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.main.components.tabs.account.AccountViewModel
import com.nunchuk.android.main.components.tabs.chat.contacts.ContactViewModel
import com.nunchuk.android.main.components.tabs.wallet.WalletsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface MainViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(WalletsViewModel::class)
    fun bindWalletsViewModel(viewModel: WalletsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AccountViewModel::class)
    fun bindAccountViewModel(viewModel: AccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContactViewModel::class)
    fun bindContactViewModel(viewModel: ContactViewModel): ViewModel

}