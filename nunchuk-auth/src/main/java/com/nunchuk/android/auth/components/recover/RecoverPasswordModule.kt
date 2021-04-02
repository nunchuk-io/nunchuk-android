package com.nunchuk.android.auth.components.recover

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal interface RecoverPasswordModule {

    @Binds
    @IntoMap
    @ViewModelKey(RecoverPasswordViewModel::class)
    fun bindChangePasswordViewModel(viewModel: RecoverPasswordViewModel): ViewModel

}