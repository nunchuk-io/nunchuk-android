package com.nunchuk.android.wallet.assign

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface AssignSignerModule {

    @Binds
    @IntoMap
    @ViewModelKey(AssignSignerViewModel::class)
    fun bindAssignSignerViewModel(viewModel: AssignSignerViewModel): ViewModel

}