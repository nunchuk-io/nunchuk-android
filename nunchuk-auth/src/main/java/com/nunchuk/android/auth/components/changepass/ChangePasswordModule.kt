package com.nunchuk.android.auth.components.changepass

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface ChangePasswordModule {

    @Binds
    @IntoMap
    @ViewModelKey(ChangePasswordViewModel::class)
    fun bindChangePasswordViewModel(viewModel: ChangePasswordViewModel): ViewModel

}