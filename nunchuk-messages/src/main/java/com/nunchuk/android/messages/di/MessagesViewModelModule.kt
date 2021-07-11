package com.nunchuk.android.messages.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.messages.contact.AddContactsViewModel
import com.nunchuk.android.messages.room.detail.RoomDetailViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface MessagesViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(RoomDetailViewModel::class)
    fun bindWalletsViewModel(viewModel: RoomDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddContactsViewModel::class)
    fun bindAddContactsViewModel(viewModel: AddContactsViewModel): ViewModel

}