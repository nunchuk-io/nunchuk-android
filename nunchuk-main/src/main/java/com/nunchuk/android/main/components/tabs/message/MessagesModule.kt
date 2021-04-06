package com.nunchuk.android.main.components.tabs.message

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface MessagesModule {

    @Binds
    @IntoMap
    @ViewModelKey(MessageViewModel::class)
    fun bindMessageViewModel(viewModel: MessageViewModel): ViewModel

}