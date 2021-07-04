package com.nunchuk.android.main.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.main.components.tabs.account.AccountViewModel
import com.nunchuk.android.main.components.tabs.chat.contacts.ContactsViewModel
import com.nunchuk.android.main.components.tabs.chat.messages.MessagesViewModel
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
    @ViewModelKey(ContactsViewModel::class)
    fun bindContactsViewModel(viewModel: ContactsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MessagesViewModel::class)
    fun bindMessagesViewModel(viewModel: MessagesViewModel): ViewModel

}