package com.nunchuk.android.messages.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.messages.components.room.create.CreateRoomViewModel
import com.nunchuk.android.messages.components.room.detail.RoomDetailViewModel
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
    @ViewModelKey(CreateRoomViewModel::class)
    fun bindCreateRoomViewModel(viewModel: CreateRoomViewModel): ViewModel

}