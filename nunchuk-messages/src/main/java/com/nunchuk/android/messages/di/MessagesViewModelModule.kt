package com.nunchuk.android.messages.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.messages.components.create.CreateRoomViewModel
import com.nunchuk.android.messages.components.detail.RoomDetailViewModel
import com.nunchuk.android.messages.components.direct.ChatInfoViewModel
import com.nunchuk.android.messages.components.group.ChatGroupInfoViewModel
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

    @Binds
    @IntoMap
    @ViewModelKey(ChatInfoViewModel::class)
    fun bindChatInfoViewModel(viewModel: ChatInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChatGroupInfoViewModel::class)
    fun bindChatGroupInfoViewModel(viewModel: ChatGroupInfoViewModel): ViewModel

}