package com.nunchuk.android.main.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.main.SyncRoomViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ActivityViewModelModule {
    @ActivityScope
    @Binds
    @IntoMap
    @ViewModelKey(SyncRoomViewModel::class)
    fun bindSyncRoomViewModel(viewModel: SyncRoomViewModel): ViewModel

}